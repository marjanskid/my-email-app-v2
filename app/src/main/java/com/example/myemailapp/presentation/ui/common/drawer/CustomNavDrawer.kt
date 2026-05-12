package com.example.myemailapp.presentation.ui.common.drawer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myemailapp.presentation.model.NavDrawerItem
import com.example.myemailapp.presentation.model.Screen
import com.example.myemailapp.ui.theme.MyEmailAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomNavigationDrawer(
    child: @Composable () -> Unit,
    items: List<NavDrawerItem>,
    selectedItemScreenName: String,
    drawerState: DrawerState,
    onNavDrawerItemPressed: (NavDrawerItem) -> Unit
) {
    val selectedItemIndex =
        rememberSaveable { mutableIntStateOf(items.indexOf(items.first { it.screen.route == selectedItemScreenName })) }
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))

                items.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(text = item.title) },
                        selected = index == selectedItemIndex.intValue,
                        onClick = {
                            if (selectedItemIndex.intValue == index) {
                                return@NavigationDrawerItem
                            }

                            selectedItemIndex.intValue = index
                            scope.launch {
                                drawerState.close()
                            }

                            onNavDrawerItemPressed(item)
                        },
                        icon = {
                            Icon(
                                imageVector = if (index == selectedItemIndex.intValue) {
                                    item.selectedIcon
                                } else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        modifier = Modifier
                            .padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

            }
        },
        gesturesEnabled = true
    ) {
        child()
    }
}

@Preview
@Composable
fun CustomNavigationDrawerPreview() {
    MyEmailAppTheme {
        CustomNavigationDrawer(
            child = {},
            items = emailsScreenDrawerItems,
            drawerState = rememberDrawerState(initialValue = DrawerValue.Open),
            onNavDrawerItemPressed = {},
            selectedItemScreenName = "",
        )
    }
}