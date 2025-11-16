package com.example.nextscene.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    // NavController'ı buraya ekleyebilirsiniz, örneğin çıkış yapmak için
    // navController: NavController,
    viewModel: AuthViewModel = viewModel() // AuthViewModel'i al
) {
    // 1. ViewModel'den state'leri topla
    val authState by viewModel.authState.collectAsState()
    val userData by viewModel.userData.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 2. Düzenlenebilir alanlar için yerel state'ler oluştur
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    // 3. Salt okunur alanlar için state'ler
    var email by remember { mutableStateOf("") }

    // 4. Veri ViewModel'den yüklendiğinde yerel state'leri doldur
    LaunchedEffect(userData) {
        userData?.let {
            name = it.name
            surname = it.surname
            username = it.username
            bio = it.bio
            phoneNumber = it.phoneNumber
            email = it.email
        }
    }

    // 5. Güncelleme durumunu (başarı/hata) dinle
    LaunchedEffect(authState) {
        if (authState.isSuccess) {
            Toast.makeText(context, "Profil başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
            viewModel.resetState() // Hata/Başarı durumunu sıfırla
        }
        if (authState.errorMessage != null) {
            Toast.makeText(context, "Hata: ${authState.errorMessage}", Toast.LENGTH_LONG).show()
            viewModel.resetState()
        }
    }

    // 6. UI (Kullanıcı Arayüzü)
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profili Düzenle") })
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()), // Ekran taşarsa kaydırma sağlar
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Salt Okunur Alanlar
                OutlinedTextField(
                    value = email,
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false // Email değiştirilemez
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {}, // Kullanıcı adını değiştirmek isterseniz bunu güncelleyin
                    label = { Text("Kullanıcı Adı") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false // Genellikle değiştirilmez, isterseniz 'true' yapın
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Düzenlenebilir Alanlar
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Soyisim") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Telefon Numarası") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth().height(120.dp), // Çok satırlı bio için
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // 7. Kaydetme işlemini tetikle
                        val updatedData = mapOf(
                            "name" to name,
                            "surname" to surname,
                            "phoneNumber" to phoneNumber,
                            "bio" to bio
                            // Not: username'i güncellenebilir yaptıysanız buraya ekleyin
                        )
                        scope.launch {
                            viewModel.updateUserData(updatedData)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !authState.isLoading // Yüklenirken butonu devre dışı bırak
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}