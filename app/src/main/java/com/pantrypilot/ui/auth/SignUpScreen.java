package com.pantrypilot.ui.auth;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.text.KeyboardOptions;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.material3.SnackbarHostState;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.input.KeyboardType;
import androidx.compose.ui.text.input.PasswordVisualTransformation;
import androidx.navigation.NavController;

public class SignUpScreen {

    @Composable
    public static void Screen(NavController navController, AuthViewModel viewModel) {
        State<String> error = viewModel.error.observeAsState();
        State<Boolean> loading = viewModel.loading.observeAsState(false);

        MutableState<String> householdName = remember {
            mutableStateOf("")
        }
        MutableState<String> email = remember {
            mutableStateOf("")
        }
        MutableState<String> password = remember {
            mutableStateOf("")
        }
        MutableState<String> confirm = remember {
            mutableStateOf("")
        }

        SnackbarHostState snackbar = remember {
            new SnackbarHostState()
        }
        LaunchedEffect(error.getValue()) {
            String msg = error.getValue();
            if (msg != null && !msg.isEmpty()) snackbar.showSnackbar(msg);
        }

        Scaffold(
                snackbarHost = {SnackbarHost(snackbar)},
                topBar = {
                        TopAppBar(
                                title = {Text("Create Household")},
                                navigationIcon = {
                                        IconButton(onClick = () -> navController.popBackStack()){
                                        Icon(Icons.Filled.ArrowBack, "Back");
                                }
                            }
                    )
                }
        ){
            padding ->
                    Column(
                            modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(horizontal = 32.dp),
                            verticalArrangement = Arrangement.Center
                    ) {
                Text("Set up your household",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold);
                Spacer(Modifier.height(8.dp));
                Text("All family members will share this pantry.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant);
                Spacer(Modifier.height(32.dp));

                OutlinedTextField(
                        value = householdName.getValue(),
                        onValueChange = v -> householdName.setValue(v),
                        label = {Text("Household Name")},
                        placeholder = {Text("e.g. The Tingre Home")},
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth());
                Spacer(Modifier.height(12.dp));
                OutlinedTextField(
                        value = email.getValue(),
                        onValueChange = v -> email.setValue(v),
                        label = {Text("Email")},
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth());
                Spacer(Modifier.height(12.dp));
                OutlinedTextField(
                        value = password.getValue(),
                        onValueChange = v -> password.setValue(v),
                        label = {Text("Password (min 6 chars)")},
                        singleLine = true,
                        visualTransformation = new PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth());
                Spacer(Modifier.height(12.dp));
                OutlinedTextField(
                        value = confirm.getValue(),
                        onValueChange = v -> confirm.setValue(v),
                        label = {Text("Confirm Password")},
                        singleLine = true,
                        visualTransformation = new PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth());
                Spacer(Modifier.height(24.dp));

                Button(
                        onClick = () -> viewModel.signUp(
                                householdName.getValue(),
                                email.getValue(),
                                password.getValue(),
                                confirm.getValue()),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = !loading.getValue()
                ) {
                    if (loading.getValue()) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp);
                    } else {
                        Text("Create Household", fontSize = 16.sp);
                    }
                }
            }
        }
    }
}
