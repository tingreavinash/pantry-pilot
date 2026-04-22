package com.pantrypilot.ui.auth;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.text.input.PasswordVisualTransformation;
import androidx.compose.ui.text.input.VisualTransformation;
import androidx.navigation.NavController;

import com.pantrypilot.ui.common.Navigation;

public class LoginScreen {

    @Composable
    public static void Screen(NavController navController, AuthViewModel viewModel) {
        State<String> error = viewModel.error.observeAsState();
        State<Boolean> loading = viewModel.loading.observeAsState(false);

        MutableState<String> email = remember {
            mutableStateOf("")
        }
        MutableState<String> password = remember {
            mutableStateOf("")
        }
        MutableState<Boolean> showPwd = remember {
            mutableStateOf(false)
        }

        SnackbarHostState snackbar = remember {
            new SnackbarHostState()
        }

        LaunchedEffect(error.getValue()) {
            String msg = error.getValue();
            if (msg != null && !msg.isEmpty()) snackbar.showSnackbar(msg);
        }

        Scaffold(snackbarHost = {SnackbarHost(snackbar)}) {
            padding ->
                    Column(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(horizontal = 32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                Text("🌿", fontSize = 56.sp);
                Spacer(Modifier.height(8.dp));
                Text("PantryPilot",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary);
                Text("Smart home grocery management",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant);
                Spacer(Modifier.height(40.dp));

                OutlinedTextField(
                        value = email.getValue(),
                        onValueChange = v -> email.setValue(v),
                        label = {Text("Email")},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                );
                Spacer(Modifier.height(12.dp));
                OutlinedTextField(
                        value = password.getValue(),
                        onValueChange = v -> password.setValue(v),
                        label = {Text("Password")},
                        singleLine = true,
                        visualTransformation = showPwd.getValue()
                                ? VisualTransformation.None
                                : new PasswordVisualTransformation(),
                        trailingIcon = {
                                IconButton(onClick = () -> showPwd.setValue(!showPwd.getValue())){
                                Icon(showPwd.getValue()
                                                ? Icons.Filled.VisibilityOff
                                                : Icons.Filled.Visibility,
                                        "Toggle password");
                            }
                        },
                keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp));

                Button(
                        onClick = () -> viewModel.login(email.getValue(), password.getValue()),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !loading.getValue()
                ) {
                    if (loading.getValue()) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp);
                    } else {
                        Text("Sign In", fontSize = 16.sp);
                    }
                }
                Spacer(Modifier.height(16.dp));
                TextButton(
                        onClick = () -> navController.navigate(Navigation.ROUTE_SIGNUP)
                ) {
                    Text("New here? Create a household →");
                }
            }
        }
    }
}
