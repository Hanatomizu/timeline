package moe.hanatomizu.timeline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import moe.hanatomizu.timeline.ui.screens.TimelineDetailScreen
import moe.hanatomizu.timeline.ui.screens.TimelineListScreen
import moe.hanatomizu.timeline.ui.theme.TimelineTheme
import moe.hanatomizu.timeline.viewmodel.TimelineDetailViewModel

/**
 * 主入口 Activity。
 *
 * 设置 Compose 主题与导航路由：
 *   - "list" → 时间线列表
 *   - "detail/{timelineId}" → 时间线详情
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimelineTheme {
                TimelineNavGraph()
            }
        }
    }
}

@Composable
fun TimelineNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "list"
    ) {
        // 时间线列表
        composable("list") {
            TimelineListScreen(
                onNavigateToDetail = { timelineId ->
                    navController.navigate("detail/$timelineId")
                }
            )
        }

        // 时间线详情（传递 timelineId）
        composable(
            route = "detail/{timelineId}",
            arguments = listOf(
                navArgument("timelineId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val timelineId = backStackEntry.arguments?.getLong("timelineId") ?: return@composable

            // 为每个详情页创建独立的 ViewModel 作用域
            // 使用 backStackEntry 的 ViewModelStoreOwner，确保返回后重新进入时重新创建
            val detailViewModel: TimelineDetailViewModel = viewModel(
                viewModelStoreOwner = backStackEntry
            )

            TimelineDetailScreen(
                timelineId = timelineId,
                onBack = { navController.popBackStack() },
                viewModel = detailViewModel
            )
        }
    }
}
