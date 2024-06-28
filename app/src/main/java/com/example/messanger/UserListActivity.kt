package com.example.messanger

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(navController: NavHostController) {
    var query by remember { mutableStateOf("") }
    var nicknames by remember { mutableStateOf(listOf<User>()) }
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = query,
            onValueChange = {
                query = it
                searchUsers(context, query, currentUserUid) { results ->
                    nicknames = results
                }
            },
            label = { Text("Поиск пользователей") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(nicknames) { user ->
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = user.nickname)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { sendFriendRequest(context, user.uid) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отправить запрос")
                    }
                }
            }
        }
    }
}

private fun searchUsers(context: Context, query: String, currentUserUid: String, onResults: (List<User>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userReference = db.collection("users")

    if (query.isNotEmpty()) {
        val lowercaseQuery = query.toLowerCase(Locale.getDefault())

        userReference.get()
            .addOnSuccessListener { querySnapshot ->
                val results = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(User::class.java)
                }.filter { user ->
                    user.uid != currentUserUid && (
                            user.nickname.toLowerCase(Locale.getDefault()).contains(lowercaseQuery) ||
                                    user.lastName.toLowerCase(Locale.getDefault()).contains(lowercaseQuery) ||
                                    user.firstName.toLowerCase(Locale.getDefault()).contains(lowercaseQuery)
                            )
                }
                onResults(results)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка поиска: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    } else {
        onResults(emptyList())
    }
}

private fun sendFriendRequest(context: Context, uid: String) {
    val db = FirebaseFirestore.getInstance()
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val userCollection = db.collection("users")
    val userQuery = userCollection.whereEqualTo("uid", uid)
    val currentUserQuery = userCollection.whereEqualTo("uid", currentUserUid)

    currentUserQuery.get()
        .addOnSuccessListener { currentUserSnapshot ->
            if (!currentUserSnapshot.isEmpty) {
                val currentUserDocument = currentUserSnapshot.documents[0]
                val currentUser = currentUserDocument.toObject(User::class.java)
                if (currentUser != null) {
                    if (currentUser.friends.contains(uid)) {
                        Toast.makeText(context, "Этот пользователь уже у вас в друзьях.", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    userQuery.get()
                        .addOnSuccessListener { querySnapshot ->
                            if (!querySnapshot.isEmpty) {
                                val documentSnapshot = querySnapshot.documents[0]
                                val user = documentSnapshot.toObject(User::class.java)
                                if (user != null) {
                                    if (!user.friendRequests.contains(currentUserUid)) {
                                        val updatedFriendRequests = user.friendRequests + currentUserUid
                                        documentSnapshot.reference.update("friendRequests", updatedFriendRequests)
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Запрос в друзья отправлен", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Ошибка запроса: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(context, "Запрос уже был отправлен.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Ошибка получения пользователя", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Ошибка отправки запроса: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Ошибка получения текущего пользователя", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Текущий пользователь не найден", Toast.LENGTH_SHORT).show()
            }
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Ошибка проверки текущего пользователя: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

