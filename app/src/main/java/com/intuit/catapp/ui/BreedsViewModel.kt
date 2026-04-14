package com.intuit.catapp.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.intuit.catapp.data.Breed
import com.intuit.catapp.data.CatService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BreedsViewModel: ViewModel() {

    init {
        // Initialize the service for API calls
        CatService.init()
    }

    //private val _breedsLiveData = MutableLiveData<BreedsResult>()
    //val breedsLiveData: LiveData<BreedsResult>
     //   get() = _breedsLiveData

    // Holds the full unfiltered list fetched from API
    // Private so UI cannot modify it directly
    private val _allBreeds = MutableStateFlow<List<Breed>>(emptyList())

    // Task 4 - Holds current search query type by user
    // Private mutable, public read-only expose as StateFlow
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Task 4 - Derived filtered list using combine()
    // combine() merges _allBreeds and _searchQuery into
    // one output - automatically recomputes when EITHER
    // source changes, no manual triggering needed.
    // stateIn() converts the Flow to StateFlow so composable
    // can collect it efficiently as Compose State
    val filteredBreeds: StateFlow<List<Breed>> = combine(
        _allBreeds,
        _searchQuery
    ) { breeds, query ->
        // If query is blank return full list
        // otherwise filter by name case-insensitively
        if(query.isBlank())  breeds
        else breeds.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        // Keep flow active for 5 seconds after last subscriber leaves -
        // handles screen rotation without restarting
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Task 4 - Called from composable when user types in search bar
    // Simply updates _searchQuery - filteredBreeds recomputes
    // automatically via combine() - no manual re-trigger needed
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun getBreeds() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = CatService.getService()

                // Task 1 - Fetch the first page immediately and update
                // _allBreeds  go filterBreeds emits first results
                // fast - user sees data without waiting for all pages
                val firstPage = service.getBreeds(limit = 100, page = 0)
                Log.d(BreedsViewModel::class.simpleName,"$firstPage")
                if (firstPage.isEmpty()){
                    // APT returned no breeds - _allBreeds stays
                    // empty, filteredBreeds emits emptyList()
                    return@launch
                }
                // Progressive loading - Show first page immediately
                // filteredBreeds recomputes vis combine() automatically
                _allBreeds.value = firstPage

                // If first page has fewer than 100 results, all breeds
                // are already loaded - no need to paginate further
                if (firstPage.size < 100) return@launch

                // Task 1 - Paginate the remaining pages sequentially
                // until API returns empty list, ensuring ALL
                // breeds are fetched regardless of total count
                val allBreeds = firstPage.toMutableList()
                var page = 1
                while (true) {
                    val nextPage = service.getBreeds(limit = 100, page = page)
                    Log.d(BreedsViewModel::class.simpleName, "Page $page: $nextPage")
                    if (nextPage.isEmpty()) break
                    allBreeds.addAll(nextPage)
                    page++
                }
                // Update with complete list - filteredBreeds
                // recomputes automatically via combine()
                _allBreeds.value = allBreeds
            } catch (e: Exception) {
                // Log at error level since this is a failure case
                Log.e(BreedsViewModel::class.simpleName, "Error fetching breeds: ${e.message}")
            }
        }
    }

    // BreedResult no longer needed - replaced by separate
    // _allBreeds StateFlow for data. Error state can be
    // added as a separate StateFlow if needed in future
    //data class BreedsResult(val data: List<Breed>? = emptyList(), val error: String? = null)
}
