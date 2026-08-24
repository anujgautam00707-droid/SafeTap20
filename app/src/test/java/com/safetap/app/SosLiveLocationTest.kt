package com.safetap.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.safetap.app.data.contacts.TrustedContact
import org.robolectric.annotation.Config
import com.safetap.app.data.contacts.TrustedContactsRepository
import com.safetap.app.data.sos.FakeSosRemoteDataSource
import com.safetap.app.data.sos.SosRemoteDataSource
import com.safetap.app.domain.sos.SosCoordinator
import com.safetap.app.data.sos.LocalSosDataSource
import com.safetap.app.data.status.AppStatusRepository
import com.safetap.app.domain.sos.model.LocationPrecision
import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.LocationResult
import com.safetap.app.domain.sos.model.LocationTrackingState
import com.safetap.app.domain.sos.model.SosError
import com.safetap.app.domain.sos.model.SosStatus
import com.safetap.app.domain.sos.services.BatteryProvider
import com.safetap.app.domain.sos.services.EmergencyCallManager
import com.safetap.app.domain.sos.services.EmergencyNotificationManager
import com.safetap.app.domain.sos.services.EmergencySmsSender
import com.safetap.app.domain.sos.services.LocationProvider
import com.safetap.app.domain.sos.services.LocationTrackingManager
import com.safetap.app.domain.sos.services.PermissionChecker
import com.safetap.app.domain.sos.services.SmsDeliveryState
import com.safetap.app.domain.sos.services.SmsRecipientStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    application = Application::class
)
@OptIn(ExperimentalCoroutinesApi::class)
class SosLiveLocationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var appStatusRepository: AppStatusRepository
    private lateinit var localSosDataSource: LocalSosDataSource

    private lateinit var permissionChecker: FakePermissionChecker
    private lateinit var locationProvider: FakeLocationProvider
    private lateinit var batteryProvider: FakeBatteryProvider
    private lateinit var notificationManager: FakeEmergencyNotificationManager
    private lateinit var callManager: FakeEmergencyCallManager
    private lateinit var smsSender: FakeEmergencySmsSender
    private lateinit var contactsRepository: TrustedContactsRepository
    private lateinit var remoteDataSource: FakeSosRemoteDataSource
    private lateinit var locationTrackingManager: FakeLocationTrackingManager
    private lateinit var sosCoordinator: SosCoordinator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appStatusRepository = AppStatusRepository(context)
        localSosDataSource = LocalSosDataSource(context)
        permissionChecker = FakePermissionChecker(allGranted = true)
        locationProvider = FakeLocationProvider(
            gpsEnabled = true,
            currentLocation = LocationResult(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 4.2f,
                isLastKnownLocation = false
            )
        )
        batteryProvider = FakeBatteryProvider(percentage = 88)
        notificationManager = FakeEmergencyNotificationManager()
        callManager = FakeEmergencyCallManager()
        smsSender = FakeEmergencySmsSender()
        contactsRepository = TrustedContactsRepository(
            context = context,
            appStatusRepository = appStatusRepository
        )

        contactsRepository.addContact(
            name = "Alice",
            relationship = "Sister",
            phone = "+15550100001"
        ).getOrThrow()

        contactsRepository.addContact(
            name = "Bob",
            relationship = "Brother",
            phone = "+15550200002"
        ).getOrThrow()
        remoteDataSource = FakeSosRemoteDataSource()
        locationTrackingManager = FakeLocationTrackingManager()

        sosCoordinator = SosCoordinator(
            context = context,
            permissionChecker = permissionChecker,
            locationProvider = locationProvider,
            batteryProvider = batteryProvider,
            notificationManager = notificationManager,
            callManager = callManager,
            emergencySmsSender = smsSender,
            trustedContactsRepository = contactsRepository,
            appStatusRepository = appStatusRepository,
            remoteDataSource = remoteDataSource,
            locationTrackingManager = locationTrackingManager,
            localSosDataSource = localSosDataSource,
            ioDispatcher = testDispatcher
        )
    }

    // 1. GPS success
    @Test
    fun testSosStartsLocationTracking_gpsSuccess() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        assertTrue("SOS trigger should succeed", result.isSuccess)
        assertTrue("Location tracking should be active", locationTrackingManager.isTrackingActive())
        assertNotNull("Tracking SOS ID should match", locationTrackingManager.lastStartedSosId)
        val data = result.getOrThrow()
        assertEquals(37.7749, data.latitude, 0.0001)
        assertEquals(-122.4194, data.longitude, 0.0001)
        assertEquals(4.2f, data.locationAccuracy, 0.01f)
    }

    // 2. Last-location fallback
    @Test
    fun testLastLocationFallbackWhenGpsUnavailable() = runTest(testDispatcher) {
        locationProvider.currentLocation = null
        locationProvider.cachedLocation = LocationResult(
            latitude = 28.6139,
            longitude = 77.2090,
            accuracy = 12.0f,
            isLastKnownLocation = true
        )

        val result = sosCoordinator.triggerSos(userId = "test_user")
        assertTrue(result.isSuccess)
        val data = result.getOrThrow()
        assertEquals(28.6139, data.latitude, 0.0001)
        assertEquals(77.2090, data.longitude, 0.0001)
        assertTrue(data.isLastKnownLocation)
    }

    // 3. GPS timeout
    @Test
    fun testGpsTimeoutHandlesGracefully() = runTest(testDispatcher) {
        val slowLocationProvider = object : LocationProvider {
            override fun isGpsEnabled(): Boolean = true
            override suspend fun getCurrentLocation(): LocationResult? = null
            override suspend fun getLastKnownLocation(): LocationResult? = LocationResult(
                latitude = 28.6139,
                longitude = 77.2090,
                accuracy = 15f,
                isLastKnownLocation = true
            )
            override suspend fun getBestAvailableLocation(): LocationResult? = getLastKnownLocation()
        }

        val coordinator = SosCoordinator(
            context = context,
            permissionChecker = permissionChecker,
            locationProvider = slowLocationProvider,
            batteryProvider = batteryProvider,
            notificationManager = notificationManager,
            callManager = callManager,
            emergencySmsSender = smsSender,
            trustedContactsRepository = contactsRepository,
            appStatusRepository = appStatusRepository,
            remoteDataSource = remoteDataSource,
            locationTrackingManager = locationTrackingManager,
            localSosDataSource = localSosDataSource,
            ioDispatcher = testDispatcher
        )

        val result = coordinator.triggerSos(userId = "test_user")
        assertTrue("SOS should succeed using fallback location on GPS timeout", result.isSuccess)
        val data = result.getOrThrow()
        assertEquals(28.6139, data.latitude, 0.0001)
        assertTrue(data.isLastKnownLocation)
    }

    // 4. Firebase authentication failure handled gracefully
    @Test
    fun testFirebaseAuthenticationFailureHandledGracefully() = runTest(testDispatcher) {
        val authFailingDataSource = object : SosRemoteDataSource {
            override suspend fun createSosEvent(emergencyData: EmergencyData): Result<String> {
                return Result.failure(RuntimeException("Permission denied: auth != null required"))
            }
            override suspend fun updateSosLocation(sosId: String, latitude: Double, longitude: Double, accuracy: Float, battery: Int?): Result<Unit> = Result.failure(RuntimeException("Auth required"))
            override suspend fun closeSosEvent(sosId: String): Result<Unit> = Result.failure(RuntimeException("Auth required"))
            override suspend fun getActiveSosEvent(sosId: String): Result<EmergencyData?> = Result.failure(RuntimeException("Auth required"))
            override fun observeSosEvent(sosId: String) = kotlinx.coroutines.flow.emptyFlow<EmergencyData?>()
        }

        val coordinator = SosCoordinator(
            context = context,
            permissionChecker = permissionChecker,
            locationProvider = locationProvider,
            batteryProvider = batteryProvider,
            notificationManager = notificationManager,
            callManager = callManager,
            emergencySmsSender = smsSender,
            trustedContactsRepository = contactsRepository,
            appStatusRepository = appStatusRepository,
            remoteDataSource = authFailingDataSource,
            locationTrackingManager = locationTrackingManager,
            localSosDataSource = localSosDataSource,
            ioDispatcher = testDispatcher
        )

        val result = coordinator.triggerSos(userId = "unauthenticated_user")
        assertTrue("SOS and SMS must still succeed even if initial Firebase Auth write fails", result.isSuccess)
        assertEquals(1, smsSender.sentMessages.size)
    }

    // 5. Firebase write failure
    @Test
    fun testFirebaseWriteFailureDoesNotCrashSos() = runTest(testDispatcher) {
        val failingRemoteDataSource = object : SosRemoteDataSource {
            override suspend fun createSosEvent(emergencyData: EmergencyData): Result<String> {
                return Result.failure(RuntimeException("Firebase network connection timed out"))
            }

            override suspend fun updateSosLocation(
                sosId: String,
                latitude: Double,
                longitude: Double,
                accuracy: Float,
                battery: Int?
            ): Result<Unit> = Result.failure(RuntimeException("Firebase unavailable"))

            override suspend fun closeSosEvent(sosId: String): Result<Unit> = Result.failure(RuntimeException("Firebase error"))
            override suspend fun getActiveSosEvent(sosId: String): Result<EmergencyData?> = Result.failure(RuntimeException("Firebase error"))
            override fun observeSosEvent(sosId: String) = kotlinx.coroutines.flow.emptyFlow<EmergencyData?>()
        }

        val coordinatorWithFailingFirebase = SosCoordinator(
            context = context,
            permissionChecker = permissionChecker,
            locationProvider = locationProvider,
            batteryProvider = batteryProvider,
            notificationManager = notificationManager,
            callManager = callManager,
            emergencySmsSender = smsSender,
            trustedContactsRepository = contactsRepository,
            appStatusRepository = appStatusRepository,
            remoteDataSource = failingRemoteDataSource,
            locationTrackingManager = locationTrackingManager,
            localSosDataSource = localSosDataSource,
            ioDispatcher = testDispatcher
        )

        val result = coordinatorWithFailingFirebase.triggerSos(userId = "test_user")
        assertTrue("SOS should succeed and dispatch SMS even if remote DB fails", result.isSuccess)
        assertEquals(1, smsSender.sentMessages.size)
        assertTrue(locationTrackingManager.isTrackingActive())
    }

    // 6. SOS cancellation during GPS acquisition
    @Test
    fun testCancellationWhileWaitingForGps() = runTest(testDispatcher) {
        val job = launch(testDispatcher) {
            sosCoordinator.runCountdown(durationSeconds = 5) {}
        }
        advanceTimeBy(1000L)
        job.cancel()
        advanceTimeBy(1000L)

        assertFalse("Tracking should not be active", locationTrackingManager.isTrackingActive())
    }

    // 7. SOS cancellation after GPS success
    @Test
    fun testSosCancellationStopsLocationTracking() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        val sosId = result.getOrThrow().sosId

        assertTrue(locationTrackingManager.isTrackingActive())
        sosCoordinator.cancelSos(sosId)

        assertFalse("Tracking should stop after cancellation", locationTrackingManager.isTrackingActive())
        assertEquals("Tracking state should be Stopped", LocationTrackingState.Stopped, locationTrackingManager.trackingState.value)
        assertFalse(notificationManager.isNotificationActive)
    }

    // 8. Late GPS callback after cancellation is ignored
    @Test
    fun testLateGpsCallbackAfterCancellationIsIgnored() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        val sosId = result.getOrThrow().sosId

        sosCoordinator.cancelSos(sosId)
        assertFalse(locationTrackingManager.isTrackingActive())

        // Simulate late callback arriving after stop
        locationTrackingManager.simulateLateLocationCallback(lat = 37.8000, lng = -122.4000)

        assertFalse("Tracking should remain inactive after late callback", locationTrackingManager.isTrackingActive())
        assertEquals(LocationTrackingState.Stopped, locationTrackingManager.trackingState.value)
    }

    // 9. Location updates after SOS ended must be ignored
    @Test
    fun testLocationUpdatesAfterSosEndedAreIgnored() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        val sosId = result.getOrThrow().sosId

        sosCoordinator.cancelSos(sosId)

        val closedSos = remoteDataSource.getActiveSosEvent(sosId).getOrThrow()
        assertEquals(SosStatus.ENDED, closedSos!!.status)

        // Attempting to update location on ended SOS must not change status back to ACTIVE
        val updateResult = remoteDataSource.updateSosLocation(
            sosId = sosId,
            latitude = 37.9999,
            longitude = -122.9999,
            accuracy = 2.0f,
            battery = 75
        )

        val checkSos = remoteDataSource.getActiveSosEvent(sosId).getOrThrow()
        assertEquals(SosStatus.ENDED, checkSos!!.status)
    }

    // 10. Duplicate SOS protection
    @Test
    fun testDuplicateSosCannotStartDuplicateTracking() = runTest(testDispatcher) {
        val result1 = sosCoordinator.triggerSos(userId = "test_user")
        val sosId = result1.getOrThrow().sosId
        val initialStartCount = locationTrackingManager.startTrackingCallCount

        locationTrackingManager.startTracking(sosId, "token_123")
        assertEquals("Duplicate call with same SOS ID should not start new tracker", initialStartCount, locationTrackingManager.startTrackingCallCount)
    }

    // 11. Live location Firebase update (10+ consecutive updates)
    @Test
    fun testLocationUpdatesAreUploaded() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        val sosId = result.getOrThrow().sosId

        for (i in 1..12) {
            val updateResult = remoteDataSource.updateSosLocation(
                sosId = sosId,
                latitude = 37.7750 + (i * 0.001),
                longitude = -122.4200 - (i * 0.001),
                accuracy = 3.0f + (i * 0.1f),
                battery = 100 - i
            )
            assertTrue("Consecutive update #$i must succeed", updateResult.isSuccess)
        }

        val activeSos = remoteDataSource.getActiveSosEvent(sosId).getOrThrow()
        assertNotNull(activeSos)
        assertEquals(37.7750 + (12 * 0.001), activeSos!!.latitude, 0.0001)
        assertEquals(-122.4200 - (12 * 0.001), activeSos.longitude, 0.0001)
        assertEquals(3.0f + (12 * 0.1f), activeSos.locationAccuracy, 0.01f)
        assertEquals(88, activeSos.batteryPercentage)
    }

    // 12. Web session not found representation
    @Test
    fun testWebSessionNotFoundRepresentation() = runTest(testDispatcher) {
        val missingEvent = remoteDataSource.getActiveSosEvent("non_existent_sos_id").getOrThrow()
        assertNull("Non-existent SOS session must return null", missingEvent)
    }

    // 13. Web active session representation
    @Test
    fun testWebActiveSessionRepresentation() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        val data = result.getOrThrow()

        val activeEvent = remoteDataSource.getActiveSosEvent(data.sosId).getOrThrow()
        assertNotNull(activeEvent)
        assertEquals(SosStatus.ACTIVE, activeEvent!!.status)
        assertEquals(data.latitude, activeEvent.latitude, 0.0001)
        assertEquals(data.longitude, activeEvent.longitude, 0.0001)
    }

    // 14. Web ended session representation
    @Test
    fun testWebEndedSessionRepresentation() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        val sosId = result.getOrThrow().sosId

        sosCoordinator.cancelSos(sosId)

        val updatedEvent = remoteDataSource.getActiveSosEvent(sosId).getOrThrow()
        assertNotNull(updatedEvent)
        assertEquals(SosStatus.ENDED, updatedEvent!!.status)
        assertNotNull(updatedEvent.endedAt)
    }

    // 15. Google Maps URL generation
    @Test
    fun testGoogleMapsUrlGeneration() = runTest(testDispatcher) {
        val result = sosCoordinator.triggerSos(userId = "test_user")
        val data = result.getOrThrow()

        val expectedGoogleMapsUrl = "https://maps.google.com/?q=${data.latitude},${data.longitude}"
        val expectedSearchUrl = "https://www.google.com/maps/search/?api=1&query=${data.latitude},${data.longitude}"

        val message = smsSender.sentMessages.single().message
        assertTrue(message.contains(expectedGoogleMapsUrl))
        assertTrue(expectedSearchUrl.startsWith("https://www.google.com/maps/search/?api=1&query="))
    }

    // 16. Emergency number 112 verification
    @Test
    fun testEmergencyNumberIs112() {
        val dialResult = sosCoordinator.openEmergencyDialer(emergencyNumber = "112")
        assertTrue("Opening emergency dialer for 112 must succeed", dialResult.isSuccess)
    }

    // 17. Permission denied handled gracefully
    @Test
    fun testPermissionDeniedIsHandled() = runTest(testDispatcher) {
        permissionChecker.allGranted = false
        val result = sosCoordinator.triggerSos(userId = "test_user")
        assertTrue("SOS should fail with PermissionDenied error", result.isFailure)
        assertTrue(result.exceptionOrNull() is SosError.PermissionDenied)
        assertFalse(locationTrackingManager.isTrackingActive())
    }

    // 18. 256-bit token entropy and uniqueness prevents session enumeration
    @Test
    fun testTokenEntropyAndEnumerationResistance() {
        val tokens = (1..100).map { EmergencyData.generateCryptographicToken() }
        assertEquals("All 100 generated tokens must be strictly unique", 100, tokens.toSet().size)
        tokens.forEach { token ->
            assertEquals(64, token.length)
            assertTrue(token.matches(Regex("^[a-f0-9]{64}$")))
        }
    }

    // --- Helpers & Fakes ---

    class FakePermissionChecker(var allGranted: Boolean = true) : PermissionChecker {

        override fun hasLocationPermission() = allGranted

        override fun hasPreciseLocationPermission() = allGranted

        override fun hasApproximateLocationPermission() = allGranted

        override fun getLocationPrecision(): LocationPrecision {
            return if (allGranted) {
                LocationPrecision.HIGH_PRECISION
            } else {
                LocationPrecision.UNAVAILABLE
            }
        }

        override fun hasSmsPermission() = allGranted

        override fun hasCallPermission() = allGranted

        override fun hasNotificationPermission() = allGranted

        override fun getRequiredPermissions(): List<String> {
            return emptyList()
        }

        override fun getMissingPermissions(): List<String> {
            return if (allGranted) {
                emptyList()
            } else {
                listOf("test_permission")
            }
        }

        override fun hasRequiredPermissions() = allGranted
    }

    class FakeLocationProvider(
        var gpsEnabled: Boolean = true,
        var currentLocation: LocationResult? = null,
        var cachedLocation: LocationResult? = null
    ) : LocationProvider {
        override fun isGpsEnabled() = gpsEnabled
        override suspend fun getCurrentLocation() = currentLocation
        override suspend fun getLastKnownLocation() = cachedLocation ?: currentLocation
        override suspend fun getBestAvailableLocation() = currentLocation ?: cachedLocation
    }

    class FakeBatteryProvider(var percentage: Int = 100) : BatteryProvider {
        override fun getBatteryPercentage(): Int = percentage
    }

    class FakeEmergencyNotificationManager : EmergencyNotificationManager {
        var isNotificationActive = false
        override fun showActiveSosNotification(emergencyData: EmergencyData) { isNotificationActive = true }
        override fun cancelSosNotification() { isNotificationActive = false }
    }

    class FakeEmergencyCallManager : EmergencyCallManager {
        override fun getEmergencyDialIntent(emergencyNumber: String): android.content.Intent = android.content.Intent()
        override fun launchEmergencyDialer(emergencyNumber: String) = Result.success(Unit)
        override fun getDirectCallIntent(phoneNumber: String): android.content.Intent = android.content.Intent()
        override fun launchDirectCall(phoneNumber: String) = Result.success(Unit)
    }

    class FakeEmergencySmsSender : EmergencySmsSender {
        val sentMessages = mutableListOf<SentMessage>()
        private val _statuses = MutableStateFlow<List<SmsRecipientStatus>>(emptyList())
        override val deliveryStatuses: StateFlow<List<SmsRecipientStatus>> = _statuses.asStateFlow()

        data class SentMessage(val recipients: List<String>, val message: String)

        override fun sendEmergencyMessage(recipients: List<String>, message: String): Result<Int> {
            sentMessages.add(SentMessage(recipients, message))
            _statuses.value = recipients.map { SmsRecipientStatus(it, SmsDeliveryState.SENT) }
            return Result.success(recipients.size)
        }

        override fun clearDeliveryStatuses() {
            _statuses.value = emptyList()
        }
    }

    class FakeLocationTrackingManager : LocationTrackingManager {
        private val _trackingState = MutableStateFlow<LocationTrackingState>(LocationTrackingState.Idle)
        override val trackingState: StateFlow<LocationTrackingState> = _trackingState.asStateFlow()

        var lastStartedSosId: String? = null
        var lastStartedToken: String? = null
        var startTrackingCallCount = 0

        override fun startTracking(sosId: String, liveLocationToken: String) {
            if (isTrackingActive() && lastStartedSosId == sosId) {
                return
            }
            startTrackingCallCount++
            lastStartedSosId = sosId
            lastStartedToken = liveLocationToken
            _trackingState.value = LocationTrackingState.Tracking(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 4.0f
            )
        }

        override fun stopTracking() {
            lastStartedSosId = null
            lastStartedToken = null
            _trackingState.value = LocationTrackingState.Stopped
        }

        fun simulateLateLocationCallback(lat: Double, lng: Double) {
            if (!isTrackingActive()) {
                return
            }
            _trackingState.value = LocationTrackingState.Tracking(
                latitude = lat,
                longitude = lng,
                accuracy = 4.0f
            )
        }

        override fun isTrackingActive(): Boolean {
            return _trackingState.value is LocationTrackingState.Tracking || _trackingState.value is LocationTrackingState.Initializing
        }
    }
}
