# Stockly — Presentation Cheat Sheet
**Professor Zoom Session | June 21 2026**

---

## App at a Glance

**Stockly** is a stock market tracker built with native Android (Kotlin).  
It fetches live data from the **Finnhub API**, stores data locally with **Room**, and follows strict **MVVM** architecture.

**6 Screens (Fragments):**
| Screen | What it does |
|---|---|
| **MarketFragment** | 2-col grid of stocks, search, featured card, add/delete stocks |
| **StockDetailFragment** | Full detail: price, % change, news, watchlist toggle, navigate to trade |
| **WatchlistFragment** | Saved stocks list, swipe-left=delete, swipe-right=edit notes |
| **TradeFragment** | Order form (shares, order type, limit price), simulated trade |
| **PriceAlertFragment** | Set & manage price alerts for 10 default tickers |
| **MarketSnapshotFragment** | Take photo + GPS location, logs watchlist prices at that moment |

---

## Architecture: MVVM

```
Fragment / Activity  (UI only — observe, click, navigate)
        ↓ calls
   ViewModel         (business logic, holds state, survives rotation)
        ↓ calls
   Repository        (single source of truth: decides Room vs API)
      ↙       ↘
 Room DB      Retrofit API
(local)       (remote)
```

**Rule:** Fragments never talk to Room or Retrofit directly. Zero logic in Fragments.

---

## Android Lifecycle — Fragment

```
onAttach → onCreate → onCreateView → onViewCreated → onStart → onResume
                                                                   ↓ (user sees screen)
                                                               onPause → onStop → onDestroyView → onDestroy → onDetach
```

**Key methods used in this app:**
- `onViewCreated()` — where we set up observers, click listeners, RecyclerView
- `viewLifecycleOwner` — passed to LiveData observers so they auto-cancel when view is destroyed
- ViewModel survives `onDestroyView` → `onCreateView` (screen rotation) because it lives in a separate scope

---

## Key Libraries

### Retrofit — HTTP Client
```kotlin
// Declare the interface
interface StockApiService {
    @GET("quote")
    suspend fun getQuote(@Query("symbol") symbol: String, @Query("token") token: String): Quote
}

// Use it in a coroutine
val quote = apiService.getQuote("AAPL", API_KEY)
```
Think of it like `fetch()` in React Native but type-safe and auto-parses JSON into data classes.

### Room — Local SQLite Database
```kotlin
@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val logoUrl: String,
    val notes: String = ""
)

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY symbol ASC")
    fun getAllWatchlist(): Flow<List<WatchlistEntity>>  // auto-updates on change
    
    @Insert(onConflict = REPLACE)
    suspend fun insertWatchlistItem(item: WatchlistEntity)
}
```
Think of it like AsyncStorage but relational, type-safe, and reactive.

### LiveData & Flow — Reactive State
```kotlin
// In ViewModel
val watchlist: LiveData<List<WatchlistEntity>> = repository.getAllWatchlist().asLiveData()

// In Fragment (onViewCreated)
viewModel.watchlist.observe(viewLifecycleOwner) { list ->
    adapter.submitList(list)  // UI updates automatically
}
```
`Flow` = async stream of values (like RxJS Observable or async generator).  
`LiveData` = lifecycle-aware wrapper — observers auto-pause when Fragment is stopped.

### Hilt — Dependency Injection
```kotlin
// Mark the class as injectable
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val stocksRepo: StocksRepository,
    private val watchlistRepo: WatchlistRepository
) : ViewModel()

// In Fragment — Hilt creates and injects it automatically
private val viewModel: MarketViewModel by viewModels()
```
Hilt wires everything together. No `new ViewModel(repo1, repo2)` anywhere.

### Coroutines — Async without callbacks
```kotlin
viewModelScope.launch {          // runs on ViewModel's scope
    val profile = async { api.getCompanyProfile(symbol, key) }
    val quote = async { api.getQuote(symbol, key) }
    val result = Pair(profile.await(), quote.await())  // parallel fetch
}
```
`viewModelScope.launch` = coroutine tied to ViewModel lifetime.  
`suspend` = can pause without blocking the thread.  
`async`/`await` = parallel execution (like `Promise.all`).

### Glide — Image Loading
```kotlin
Glide.with(imageView.context)
    .load(logoUrl)
    .placeholder(R.drawable.ic_placeholder)
    .into(imageView)
```
Handles caching, placeholder, error states. Think of it like `<Image source={{uri}}>` in React Native.

### Navigation Component
```kotlin
// Navigate with bundle arguments
val bundle = Bundle().apply {
    putString("symbol", stock.symbol)
    putString("name", stock.name)
}
findNavController().navigate(R.id.action_homeFragment_to_stockDetailFragment, bundle)

// Receive in destination
val symbol = arguments?.getString("symbol") ?: ""
```
Single `NavHostFragment` in `MainActivity`. All 6 fragments registered in `nav_graph.xml`. Back stack managed automatically.

---

## Room Entities (4 tables)

| Table | PK | Key fields |
|---|---|---|
| `watchlist` | `symbol` (String) | name, logoUrl, notes |
| `user_stocks` | `id` (Int, auto) | symbol, name, logoUrl |
| `price_alerts` | `id` (Int, auto) | symbol, condition, targetPrice, logoUrl, createdAt |
| `snapshots` | `id` (Int, auto) | description, photoUri, latitude, longitude, createdAt |

---

## API Endpoints (Finnhub)

Base URL: `https://finnhub.io/api/v1/`

| Endpoint | Used for |
|---|---|
| `GET /search?q={query}` | Symbol search in AddStockBottomSheet |
| `GET /stock/profile2?symbol={s}` | Company logo, name, industry, web URL |
| `GET /quote?symbol={s}` | Current price, change %, high/low/open/prev close |
| `GET /company-news?symbol={s}&from={}&to={}` | Recent news articles with images |

API Key: hardcoded in `StocksRepository` companion object.  
Default 10 tickers: `AAPL, GOOGL, MSFT, AMZN, TSLA, META, NVDA, NFLX, AMD, INTC`

---

## Resource<T> — State Wrapper

```kotlin
sealed class Resource<T> {
    class Loading<T> : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val data: T? = null) : Resource<T>()
}
```
Every async operation returns `Resource`. Fragments switch on it:
```kotlin
when (state) {
    is Resource.Loading -> showProgressBar()
    is Resource.Success -> showData(state.data)
    is Resource.Error   -> showError(state.message)
}
```

---

## Dependency Injection Graph

```
NetworkModule (Singleton)          DatabaseModule (Singleton)
  ↓ provides                         ↓ provides
  OkHttpClient                       AppDatabase
  Retrofit                           WatchlistDao
  StockApiService                    UserStockDao
                                     PriceAlertDao
                                     SnapshotDao

Repositories (@Singleton, @Inject constructor) — auto-injected by Hilt
  StocksRepository        (API + UserStocksDao)
  WatchlistRepository     (WatchlistDao)
  PriceAlertRepository    (PriceAlertDao + StockApiService)
  SnapshotRepository      (SnapshotDao)
  UserStocksRepository    (UserStocksDao)
```

---

## Camera + Location (MarketSnapshotFragment)

**Camera flow:**
1. `PermissionUtils.hasCameraPermission(context)` → request if needed
2. `FileProvider.getUriForFile()` → creates a temp file URI
3. `takePictureLauncher.launch(photoUri)` → opens camera
4. `result == true` → photo saved to `photoUri`, enable submit

**Location flow:**
1. Check `ACCESS_FINE_LOCATION` permission
2. `fusedLocationClient.lastLocation.await()` (coroutine-friendly)
3. Store `Pair(lat, lng)` in ViewModel

**Permission denial handling:** Shows rationale dialog → if still denied, shows "go to settings" message.

---

## Swipe Gestures (ItemTouchHelper)

Used in **WatchlistFragment** and **PriceAlertFragment**:
```
Swipe LEFT  = DELETE (red background + trash icon drawn on Canvas)
Swipe RIGHT = EDIT   (gray background + edit icon)
```
Implemented by overriding `ItemTouchHelper.SimpleCallback.onSwiped()` and `onChildDraw()`.

---

## ViewBinding (no data binding, no Compose)

```kotlin
private var _binding: FragmentMarketBinding? = null
private val binding get() = _binding!!

override fun onCreateView(...): View {
    _binding = FragmentMarketBinding.inflate(inflater, container, false)
    return binding.root
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null    // prevent memory leak
}
```

---

## Common Interview Questions

**Q: Why ViewModel?**  
A: Survives configuration changes (rotation). Fragment is destroyed and re-created on rotation, but ViewModel is not.

**Q: Why Flow instead of just LiveData from Room?**  
A: Room returns `Flow<List<T>>` — it automatically emits a new list whenever the DB changes. We convert to LiveData with `.asLiveData()` for lifecycle awareness in Fragments.

**Q: Why Hilt?**  
A: Manual DI is fragile and verbose. Hilt generates the boilerplate and ensures the correct singleton instances are shared everywhere.

**Q: What is `viewLifecycleOwner` vs `this` in observe()?**  
A: `this` = Fragment's lifecycle (longer). `viewLifecycleOwner` = the view's lifecycle (shorter). Using `this` can cause updates to reach a destroyed view and crash.

**Q: Why `fallbackToDestructiveMigration()` in Room?**  
A: During development, we bump the DB version without writing a migration script — Room just drops and recreates the tables. Not safe for production, fine for a course project.

**Q: How does `combinedList` work in MarketViewModel?**  
A: It's a nested `switchMap` — outer reacts to user stocks from Room Flow, inner merges them with API stocks. Local stocks are prepended to the API list so they appear first.

---

## Manifest Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```
Camera + Location are **runtime permissions** (must ask user at runtime, not just declare in manifest).

---

*Package: `com.example.stockly` | Min SDK: 26 | Target SDK: 35 | Room DB v3*
