package com.mapxus.positioning.sample_app.page.positioning.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.mapxus.common.ui.lib.utils.DataStatus
import com.mapxus.map.mapxusmap.api.services.BuildingSearch
import com.mapxus.map.mapxusmap.api.services.model.GlobalSearchOption
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.positioning.sample_app.ui.component.LoadingCircle
import com.mapxus.positioning.sample_app.ui.component.OutlinedTextFieldCommon
import com.mapxus.positioning.sample_app.ui.component.TextButtonBig
import com.mapxus.positioning.sample_app.ui.component.TitleText
import com.mapxus.positioning.sample_app.utils.getName
import com.mapxus.positioning.sample_app.utils.showToast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Preview
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SearchContainer(
    viewModel: SearchContainerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onCancelButtonClick: () -> Unit = {},
    onSearchResultItemClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val searchContainerUiState by viewModel.searchContainerUiState.collectAsState()

    ErrorMessage(searchContainerUiState, context)

    LoadingCircle(searchContainerUiState.isLoading)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .height(600.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        OutlinedTextFieldCommon(
            fillMaxWidth = 1f,
            value = searchContainerUiState.keyword,
            labelText = "keyword",
            keyboardType = KeyboardType.Text,
            onTextChange = {
                viewModel.keywordChangeEvent(value = it)
            },
        )

        Row {
            TextButtonBig(text = "Search") {
                viewModel.search()
            }
            Spacer(modifier = Modifier.width(20.dp))
            TextButtonBig(text = "Cancel", onClick = onCancelButtonClick)
        }

        if (searchContainerUiState.buildings is DataStatus.Success) {
            val result = remember {
                (searchContainerUiState.buildings as DataStatus.Success<List<IndoorBuildingInfo>>).data
            }
            LazyColumn(
                contentPadding = PaddingValues(20.dp)
            ) {
                items(result) {
                    ListItem(modifier = Modifier.clickable(onClick = {
                        onSearchResultItemClick(
                            it.buildingId
                        )
                    })) {
                        TitleText(text = it.buildingNamesMap?.getName() ?: it.nameMap.getName())
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    searchContainerUiState: SearchContainerUiState,
    context: Context
) {
    LaunchedEffect(searchContainerUiState.buildings) {
        if (searchContainerUiState.buildings is DataStatus.Failed) {
            val errorMessage = searchContainerUiState.buildings.errorMessage
            context.showToast(errorMessage)
        }
    }
}

class SearchContainerViewModel : ViewModel() {
    private val buildingSearch = BuildingSearch.newInstance()
    private val _searchContainerUiState: MutableStateFlow<SearchContainerUiState> =
        MutableStateFlow(SearchContainerUiState())
    val searchContainerUiState = _searchContainerUiState.asStateFlow()

    fun search() {
        _searchContainerUiState.update {
            it.copy(
                buildings = DataStatus.Loading
            )
        }
        buildingSearch.searchInGlobal(GlobalSearchOption().apply {
            keyword(_searchContainerUiState.value.keyword)
        }
        ) { buildingResult ->
            val dataStatus =
                buildingResult.takeIf { it.status == 0 && it.indoorBuildingList.isNotEmpty() }
                    ?.let {
                        DataStatus.Success(buildingResult.indoorBuildingList)
                    } ?: DataStatus.Failed("Not Found")

            _searchContainerUiState.update {
                it.copy(
                    buildings = dataStatus
                )
            }
        }
    }

    fun keywordChangeEvent(value: String) {
        _searchContainerUiState.update {
            it.copy(
                keyword = value
            )
        }
    }
}

data class SearchContainerUiState(
    val keyword: String = "",
    val buildings: DataStatus<List<IndoorBuildingInfo>> = DataStatus.Idle,
) {
    val isLoading: Boolean
        get() =
            buildings is DataStatus.Loading
}