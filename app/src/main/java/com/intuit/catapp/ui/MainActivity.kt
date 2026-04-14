package com.intuit.catapp.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.intuit.catapp.R
import com.intuit.catapp.data.Breed
import com.intuit.catapp.ui.theme.CatAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BreedsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = BreedsViewModel()

        setContent {
            // Set up to observe the ViewModel's LiveData and update the composable state as it's value changes
            /*var breedsData: List<Breed> by remember { mutableStateOf(emptyList()) }
            viewModel.breedsLiveData.observe(this) {
                breedsData = it.data ?: emptyList()
            }*/

            // Trigger GET Breeds from the API asynchronously
            viewModel.getBreeds()

            CatAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    Column {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally)
                                .padding(12.dp),
                            text = "Cat Breeds",
                            fontSize = 32.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        // Task 4 - Collect filteredBreeds and searchQuery directly
                        // from viewModel as Compose State.
                        // collectAsState() converts StateFlow to State -
                        // UI recomposes automatically when ViewModel
                        // posts new filtered results or query changes.
                        // onSearchQueryChanged forwards user input back
                        // to ViewModel - composable owns no search logic
                        BreedsList(
                            data = viewModel.filteredBreeds.collectAsState().value,
                            searchQuery = viewModel.searchQuery.collectAsState().value,
                            onSearchQueryChanged = {
                                viewModel.onSearchQueryChanged(it)
                            }
                        )
                    }
                }

                /**
                 * If you want to use traditional Android View heirarchy components, you can do so
                 * leveraging the interoperability of Compose with the `AndroidView` composable function.
                 *
                 * This allows you a way to define the `factory` param (a lambda that provides context and
                 * returns a View) to integrate Android Views around and inter-mingled with compose content.
                 *
                 * For more information, please check out the Android docs:
                 * https://developer.android.com/jetpack/compose/migrate/interoperability-apis/views-in-compose
                 */
                /*
                 AndroidView(
                    factory = { context ->
                        View(context)
                    },
                    modifier = Modifier,
                    update = { view ->
                        ...
                    }
                )
                 */
            }
        }
    }
}

@Composable
fun BreedsList(
    data: List<Breed>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedBreed: Breed? by remember { mutableStateOf(null) }

    if (showDialog && selectedBreed != null) {
        ImageDialog(breed = selectedBreed!!) {
            showDialog = false
            selectedBreed = null
        }
    }

    Column {
        // Task 4 - Search bar for filtering breed name by name.
        // value and onValueChange are fully controlled by ViewModel
        // via searchQuery StateFlow and onSearchQueryChanged callback.
        // leadingIcon provides visual search hint.
        // trailingIcon (X) appears only when query is non-blank,
        // allowing users to clear the filter with one tap - calls
        // onSearchQueryChanged("") to reset ViewModel state.
        // singleLine prevents multiple input in the search field.
        // stringResource used for all labels - support localization
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {onSearchQueryChanged(it) },
            label = { Text(stringResource(id = R.string.search_breeds)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(id = R.string.search)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.search_breeds)
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        // Task 4 - Show empty state only when user has typed a query
        // AND data is empty. The AND searchQuery.isNotBlank() check
        // prevents showing "No breeds found" on initial load before
        // API responds - list starts empty so without this check
        // the error message would flash briefly on every app launch
        if (data.isEmpty() && searchQuery.isNotBlank()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.search_breeds_empty, searchQuery),
                    color = MaterialTheme.colors.onSurface,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn {
                // key = { it.id } uses the breed's stable API primary key
                // for efficient diffing - Compose only recomposes item
                // that actually changed when the filtered list updates.
                //Without key, entire list redraws on every query change
                items(data, { it.id }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                selectedBreed = it
                                showDialog = true
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colors.surface,
                        elevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colors.secondary)
                    ) {
                        Column(modifier = Modifier.padding(all = 8.dp)) {
                            // Task 2 - Breed name - Bold for visual hierarchy
                            Text(
                                text = it.name,
                                color = MaterialTheme.colors.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // Task 2 - Description only rendered if non-null
                            // using ?.let - avoid empty Text composable in tree.
                            // maxLine=2 + Ellipsis keeps item height consistent
                            it.description?.let { desc ->
                                Text(
                                    text = desc,
                                    color = MaterialTheme.colors.onSurface,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageDialog(breed: Breed, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // wrapContentHeight allows dialog to size itself
                // based on content rather than a fixed height,
                // preventing description text from being clipped
                .wrapContentHeight(),
            color = MaterialTheme.colors.surface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, Color.Black)
        ) {
            // Task 3 - verticalScroll enables scrolling for breeds
            // with long description that exceeds screen height
            Column(modifier = Modifier.padding(all = 12.dp)
                .verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Task 3: Row with breed name nd close button.
                    // SpaceBetween pushes name to start, close to end.
                    // Close button improves UX and accessibility -
                    // users doesn't have to tap outside to dismiss
                    Text(
                        text = breed.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog"
                        )
                    }
                }
                // Task 3 - AsyncImage loads breed image from CDN URL.
                // ContentScale.Crop fills bounds cleanly without
                // empty space. Error painter shown if image fails.
                AsyncImage(
                    model = breed.getImageUrl(),
                    contentDescription = breed.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(200.dp),
                    contentScale = ContentScale.Crop,
                    onError = { _ ->
                        Log.e(
                            MainActivity::class.simpleName,
                            "Async Image failed to load for breed: ${breed.name}"
                        )
                    },
                    error = painterResource(id = R.drawable.ic_image_error)
                )

                // Task 3 - Description only renders if non-null.
                // No maxLines here unlike the list - full description
                // is appropriate in the detail view
                breed.description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
