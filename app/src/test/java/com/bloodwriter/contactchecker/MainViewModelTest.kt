import android.app.Application
import com.bloodwriter.contactchecker.data.model.ContactStatus
import com.bloodwriter.contactchecker.data.model.VerificationState
import com.bloodwriter.contactchecker.repository.ContactRepositoryImpl
import com.bloodwriter.contactchecker.telephony.ContactCheckerCallManager
import com.bloodwriter.contactchecker.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val fakeApplication = object : Application() {
        override fun getSystemService(name: String): Any? = null
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: ContactRepositoryImpl
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ContactCheckerCallManager.isSimulationMode = true
        repository = ContactRepositoryImpl(
            context = fakeApplication,
            callManager = ContactCheckerCallManager,
            externalScope = testScope
        )
        viewModel = MainViewModel(fakeApplication, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRawInputAndParsing() = runTest {
        viewModel.onRawInputChanged("+18005550199, 555-0198")
        assertEquals("+18005550199, 555-0198", viewModel.rawInputText.value)

        viewModel.parseAndAddInput()
        assertEquals("", viewModel.rawInputText.value)
        assertEquals(2, viewModel.contactList.value.size)
        assertEquals("+18005550199", viewModel.contactList.value[0].phoneNumber)
        assertEquals("5550198", viewModel.contactList.value[1].phoneNumber)
    }

    @Test
    fun testClearRawInputAndLoadSampleData() = runTest {
        viewModel.onRawInputChanged("some_text")
        viewModel.clearRawInput()
        assertEquals("", viewModel.rawInputText.value)

        viewModel.loadSampleData()
        assertTrue(viewModel.rawInputText.value.contains("+18005550199"))
    }

    @Test
    fun testPermissionGrantedState() = runTest {
        assertFalse(viewModel.permissionGranted.value)

        viewModel.updatePermissionGranted(true)
        assertTrue(viewModel.permissionGranted.value)
    }

    @Test
    fun testCountCalculations() = runTest {
        viewModel.onRawInputChanged("+18005550199, invalid_num")
        viewModel.parseAndAddInput()

        assertEquals(2, viewModel.contactList.value.size)
        assertEquals(1, viewModel.pendingCount.value)
        assertEquals(1, viewModel.invalidCount.value)
        assertEquals(0, viewModel.validCount.value)
    }

    @Test
    fun testExecutionControls() = runTest {
        viewModel.onRawInputChanged("+18005550199")
        viewModel.parseAndAddInput()

        viewModel.startVerification()
        assertEquals(VerificationState.RUNNING, viewModel.verificationState.value)

        viewModel.pauseVerification()
        assertEquals(VerificationState.PAUSED, viewModel.verificationState.value)

        viewModel.resumeVerification()
        assertEquals(VerificationState.RUNNING, viewModel.verificationState.value)

        viewModel.stopVerification()
        assertEquals(VerificationState.STOPPED, viewModel.verificationState.value)

        viewModel.resetVerification()
        assertEquals(VerificationState.IDLE, viewModel.verificationState.value)
    }

    @Test
    fun testClearAndRemoveContact() = runTest {
        viewModel.onRawInputChanged("+18005550199, 555-0198")
        viewModel.parseAndAddInput()
        assertEquals(2, viewModel.contactList.value.size)

        val firstId = viewModel.contactList.value[0].id
        viewModel.removeContact(firstId)
        assertEquals(1, viewModel.contactList.value.size)

        viewModel.clearContacts()
        assertEquals(0, viewModel.contactList.value.size)
    }

    @Test
    fun testClearContactsCompletelyRemovesQueueAndResetsState() = runTest {
        viewModel.onRawInputChanged("+18005550199, 555-0198")
        viewModel.parseAndAddInput()
        viewModel.onRawInputChanged("residual text in input")
        viewModel.startVerification()

        assertEquals(2, viewModel.contactList.value.size)
        assertEquals(VerificationState.RUNNING, viewModel.verificationState.value)

        viewModel.clearContacts()

        assertEquals(0, viewModel.contactList.value.size)
        assertEquals("", viewModel.rawInputText.value)
        assertEquals(0, viewModel.currentIndex.value)
        assertEquals(VerificationState.IDLE, viewModel.verificationState.value)
        assertEquals(0, viewModel.validCount.value)
        assertEquals(0, viewModel.invalidCount.value)
        assertEquals(0, viewModel.pendingCount.value)
        assertEquals(0, viewModel.inProgressCount.value)

        val uiState = viewModel.uiState.value
        assertEquals(0, uiState.contactList.size)
        assertEquals("", uiState.rawInputText)
        assertEquals(VerificationState.IDLE, uiState.verificationState)
        assertEquals(0, uiState.validCount)
        assertEquals(0, uiState.invalidCount)
        assertEquals(0, uiState.pendingCount)
        assertEquals(0, uiState.inProgressCount)
    }
}
