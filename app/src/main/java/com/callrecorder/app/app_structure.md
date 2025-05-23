# هيكل تطبيق تسجيل المكالمات

## نظرة عامة على هيكل المشروع

سيتم تطوير التطبيق باستخدام Kotlin كلغة برمجة أساسية مع استخدام بعض أكواد Java عند الضرورة للتوافق مع مكتبات معينة. سنستخدم نمط MVVM (Model-View-ViewModel) لهيكلة التطبيق، مما يسهل اختباره وصيانته.

## المجلدات والحزم الرئيسية

```
com.callrecorder.app/
├── activities/         # أنشطة التطبيق الرئيسية
├── adapters/           # محولات العرض للقوائم والشبكات
├── database/           # قاعدة البيانات المحلية وكائنات الوصول للبيانات
├── fragments/          # أجزاء واجهة المستخدم
├── models/             # نماذج البيانات
├── receivers/          # مستقبلات البث لأحداث النظام
├── services/           # خدمات التطبيق التي تعمل في الخلفية
├── utils/              # أدوات مساعدة وثوابت
└── viewmodels/         # نماذج العرض لكل شاشة
```

## المكونات الرئيسية

### 1. خدمة تسجيل المكالمات (CallRecorderService)

خدمة تعمل في الخلفية لمراقبة حالة المكالمات وتسجيلها:

```kotlin
class CallRecorderService : Service() {
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var outputFile: String
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // تهيئة مراقبة حالة الهاتف
        setupPhoneStateListener()
        
        // إنشاء إشعار لتشغيل الخدمة في المقدمة
        startForeground(NOTIFICATION_ID, createNotification())
        
        return START_STICKY
    }
    
    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_IDLE -> {
                        // المكالمة انتهت، إيقاف التسجيل
                        stopRecording()
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        // المكالمة بدأت، بدء التسجيل
                        startRecording(phoneNumber)
                    }
                    TelephonyManager.CALL_STATE_RINGING -> {
                        // الهاتف يرن، تحضير للتسجيل
                        prepareForRecording(phoneNumber)
                    }
                }
            }
        }
        
        // تسجيل مستمع حالة الهاتف
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }
    
    private fun startRecording(phoneNumber: String?) {
        if (isRecording) return
        
        try {
            // إنشاء مسجل الوسائط
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                
                // إنشاء ملف الإخراج
                outputFile = createOutputFile(phoneNumber)
                setOutputFile(outputFile)
                
                prepare()
                start()
            }
            
            isRecording = true
            updateNotification("جاري تسجيل المكالمة...")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في بدء التسجيل", e)
            // محاولة استخدام طريقة بديلة للتسجيل
            startAlternativeRecording(phoneNumber)
        }
    }
    
    private fun stopRecording() {
        if (!isRecording) return
        
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            
            mediaRecorder = null
            isRecording = false
            
            // حفظ معلومات التسجيل في قاعدة البيانات
            saveRecordingInfo(outputFile)
            
            updateNotification("تم حفظ التسجيل")
            
        } catch (e: Exception) {
            Log.e(TAG, "خطأ في إيقاف التسجيل", e)
        }
    }
    
    // طرق إضافية...
}
```

### 2. مستقبل مكالمات (CallReceiver)

مستقبل بث يتفاعل مع أحداث المكالمات:

```kotlin
class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED ||
            intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            
            // بدء خدمة تسجيل المكالمات
            val serviceIntent = Intent(context, CallRecorderService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
```

### 3. قاعدة البيانات (AppDatabase)

استخدام Room Database لتخزين معلومات التسجيلات:

```kotlin
@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String?,
    val contactName: String?,
    val callType: Int, // 1 للواردة، 2 للصادرة
    val filePath: String,
    val duration: Long,
    val date: Long,
    val isStarred: Boolean = false,
    val notes: String? = null
)

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY date DESC")
    fun getAllRecordings(): LiveData<List<Recording>>
    
    @Query("SELECT * FROM recordings WHERE isStarred = 1 ORDER BY date DESC")
    fun getStarredRecordings(): LiveData<List<Recording>>
    
    @Query("SELECT * FROM recordings WHERE callType = :callType ORDER BY date DESC")
    fun getRecordingsByType(callType: Int): LiveData<List<Recording>>
    
    @Insert
    suspend fun insert(recording: Recording): Long
    
    @Update
    suspend fun update(recording: Recording)
    
    @Delete
    suspend fun delete(recording: Recording)
}

@Database(entities = [Recording::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "call_recorder_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 4. مستودع التسجيلات (RecordingRepository)

طبقة وسيطة بين قاعدة البيانات ونموذج العرض:

```kotlin
class RecordingRepository(private val recordingDao: RecordingDao) {
    val allRecordings: LiveData<List<Recording>> = recordingDao.getAllRecordings()
    val starredRecordings: LiveData<List<Recording>> = recordingDao.getStarredRecordings()
    
    fun getRecordingsByType(callType: Int): LiveData<List<Recording>> {
        return recordingDao.getRecordingsByType(callType)
    }
    
    suspend fun insert(recording: Recording): Long {
        return recordingDao.insert(recording)
    }
    
    suspend fun update(recording: Recording) {
        recordingDao.update(recording)
    }
    
    suspend fun delete(recording: Recording) {
        recordingDao.delete(recording)
    }
    
    suspend fun toggleStar(recording: Recording) {
        recordingDao.update(recording.copy(isStarred = !recording.isStarred))
    }
}
```

### 5. نموذج عرض التسجيلات (RecordingsViewModel)

يربط بين واجهة المستخدم والبيانات:

```kotlin
class RecordingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RecordingRepository
    val allRecordings: LiveData<List<Recording>>
    val starredRecordings: LiveData<List<Recording>>
    
    private val _filteredRecordings = MutableLiveData<List<Recording>>()
    val filteredRecordings: LiveData<List<Recording>> = _filteredRecordings
    
    init {
        val recordingDao = AppDatabase.getDatabase(application).recordingDao()
        repository = RecordingRepository(recordingDao)
        allRecordings = repository.allRecordings
        starredRecordings = repository.starredRecordings
    }
    
    fun filterRecordingsByType(callType: Int) {
        viewModelScope.launch {
            _filteredRecordings.value = repository.getRecordingsByType(callType).value
        }
    }
    
    fun insert(recording: Recording) = viewModelScope.launch {
        repository.insert(recording)
    }
    
    fun update(recording: Recording) = viewModelScope.launch {
        repository.update(recording)
    }
    
    fun delete(recording: Recording) = viewModelScope.launch {
        repository.delete(recording)
    }
    
    fun toggleStar(recording: Recording) = viewModelScope.launch {
        repository.toggleStar(recording)
    }
    
    fun searchRecordings(query: String) = viewModelScope.launch {
        val results = allRecordings.value?.filter {
            it.contactName?.contains(query, true) == true || 
            it.phoneNumber?.contains(query) == true
        }
        _filteredRecordings.value = results ?: emptyList()
    }
}
```

### 6. النشاط الرئيسي (MainActivity)

النشاط الرئيسي الذي يستضيف الأجزاء المختلفة:

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: RecordingsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // تهيئة نموذج العرض
        viewModel = ViewModelProvider(this).get(RecordingsViewModel::class.java)
        
        // إعداد شريط التنقل السفلي
        setupBottomNavigation()
        
        // طلب الأذونات اللازمة
        requestRequiredPermissions()
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_recordings -> {
                    loadFragment(RecordingsFragment())
                    true
                }
                R.id.nav_contacts -> {
                    loadFragment(ContactsFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
        
        // تحميل الجزء الافتراضي
        binding.bottomNavigation.selectedItemId = R.id.nav_recordings
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    private fun requestRequiredPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS
        )
        
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (!allGranted) {
                // إظهار رسالة توضح أهمية الأذونات
                showPermissionsExplanationDialog()
            }
        }
    }
    
    // طرق إضافية...
    
    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }
}
```

### 7. جزء التسجيلات (RecordingsFragment)

يعرض قائمة التسجيلات:

```kotlin
class RecordingsFragment : Fragment() {
    private var _binding: FragmentRecordingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: RecordingsViewModel
    private lateinit var adapter: RecordingsAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // تهيئة نموذج العرض
        viewModel = ViewModelProvider(requireActivity()).get(RecordingsViewModel::class.java)
        
        // إعداد محول العرض
        setupRecyclerView()
        
        // إعداد البحث
        setupSearch()
        
        // إعداد التصفية
        setupFilter()
        
        // مراقبة التغييرات في البيانات
        observeData()
    }
    
    private fun setupRecyclerView() {
        adapter = RecordingsAdapter(
            onItemClick = { recording ->
                // فتح شاشة تشغيل التسجيل
                openPlaybackScreen(recording)
            },
            onStarClick = { recording ->
                // تبديل حالة النجمة
                viewModel.toggleStar(recording)
            },
            onDeleteClick = { recording ->
                // حذف التسجيل بعد تأكيد المستخدم
                confirmDelete(recording)
            }
        )
        
        binding.recyclerViewRecordings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecordingsFragment.adapter
        }
    }
    
    private fun observeData() {
        viewModel.allRecordings.observe(viewLifecycleOwner) { recordings ->
            adapter.submitList(recordings)
            binding.emptyView.isVisible = recordings.isEmpty()
        }
    }
    
    // طرق إضافية...
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### 8. محول عرض التسجيلات (RecordingsAdapter)

يتحكم في عرض عناصر التسجيلات في القائمة:

```kotlin
class RecordingsAdapter(
    private val onItemClick: (Recording) -> Unit,
    private val onStarClick: (Recording) -> Unit,
    private val onDeleteClick: (Recording) -> Unit
) : ListAdapter<Recording, RecordingsAdapter.RecordingViewHolder>(RecordingDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val binding = ItemRecordingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordingViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RecordingViewHolder(private val binding: ItemRecordingBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            
            binding.buttonStar.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStarClick(getItem(position))
                }
            }
            
            binding.buttonDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }
     
(Content truncated due to size limit. Use line ranges to read in chunks)