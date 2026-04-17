package com.student.mobile_dev_laboratory_work_2

import Contact
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.student.mobile_dev_laboratory_work_2.ui.theme.Mobile_dev_Laboratory_work_2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Mobile_dev_Laboratory_work_2Theme {
                ContactsScreen()
            }
        }
    }
}

@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val permission = Manifest.permission.READ_CONTACTS

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            contacts = context.fetchAllContacts()
        }
    }

    """Повторная загрузка списка только при изменении hasPermission"""
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = context.fetchAllContacts()
        }
    }
    """Наползание интерфейса: контент не залезает под статус бар/навигацию"""
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!hasPermission) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(R.string.no_permission_text))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { launcher.launch(permission) }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            } else {
                """Column + verticalScroll = все элементы создаются сразу"""
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.contacts_title),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (contacts.isEmpty()) {
                        Text(stringResource(R.string.no_contacts_found))
                    }

                    contacts.forEach { contact ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable { selectedContact = contact }
                        ) {
                            Text(
                                text = contact.name,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    selectedContact?.let { contact ->
        AlertDialog(
            onDismissRequest = { selectedContact = null },
            title = { Text(contact.name) },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.phones,
                            contact.phones.joinToString(", ").ifEmpty { stringResource(R.string.none) }
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(
                            R.string.emails,
                            contact.emails.joinToString(", ").ifEmpty { stringResource(R.string.none) }
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedContact = null }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}