import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun test(navController: NavController) {
    navController.popBackStack("summary", inclusive = false, saveState = true)
}
