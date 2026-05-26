package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.ChatMessage
import com.example.data.MessageSender
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchMode by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    // Filter messages based on search query
    val displayMessages = remember(messages, searchQuery, searchMode) {
        if (!searchMode || searchQuery.isEmpty()) {
            messages
        } else {
            messages.filter { it.text.contains(searchQuery, ignoreCase = true) }
        }
    }

    val isDark = isSystemInDarkTheme()
    val bgBottomColor = if (isDark) CardBackgroundDark else CardBackgroundLight
    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            bgBottomColor.copy(alpha = 0.15f)
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Circular Logo in Appbar
                            SubcomposeAsyncImage(
                                model = viewModel.appLogoUrl,
                                contentDescription = "AkinAI Logo",
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                loading = { Box(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator(strokeWidth = 2.dp) } },
                                error = { Icon(Icons.Default.Android, contentDescription = "Logo Fallback") }
                            )

                            Column {
                                Text(
                                    text = "AkinAI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Powered by Gemini",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            searchMode = !searchMode
                            if (!searchMode) viewModel.onSearchQueryChange("")
                        }) {
                            Icon(
                                imageVector = if (searchMode) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search Chat History",
                                tint = if (searchMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                            )
                        }

                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "Clear Chat History",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        IconButton(onClick = { viewModel.toggleSettings(true) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Bar Expandable
                AnimatedVisibility(
                    visible = searchMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Search inside chat...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                SubcomposeAsyncImage(
                                    model = viewModel.appLogoUrl,
                                    contentDescription = "Search Logo",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    loading = { CircularProgressIndicator(strokeWidth = 1.dp) }
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(gradientBg)
        ) {
            if (displayMessages.isEmpty()) {
                // Empty view container
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SubcomposeAsyncImage(
                        model = viewModel.appLogoUrl,
                        contentDescription = "AkinAI Central Icon",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        loading = { CircularProgressIndicator() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "AkinAI Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Crafted by Akin S. Sokpah. Explore intelligence styled with precise aesthetics. No records matched your search query.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Main Chat Stream
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(displayMessages, key = { it.id }) { msg ->
                            ChatBubbleItem(
                                message = msg,
                                viewModel = viewModel
                            )
                        }
                    }

                    // Bottom Quick Selection Labels if they clicked search or empty
                    if (messages.size <= 1) {
                        SuggestionRow(onSelectPrompt = { selectedPrompt ->
                            viewModel.onInputTextChange(selectedPrompt)
                            viewModel.sendMessage()
                        })
                    }

                    // Notification banner if api key is missing
                    if (viewModel.getEffectiveApiKey().isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Api Key Required",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Environment variable VITE_GEMINI_API_KEY is not configured yet. Press Settings to provide yours!",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                TextButton(onClick = { viewModel.toggleSettings(true) }) {
                                    Text("PROVISION", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Bottom Input Box styled exactly as a polished border capsule with soft gradient/fill
                    val footerInputBg = if (isDark) CardBackgroundDark else Color(0xFFF3EDF7)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF49454F).copy(alpha = 0.4f) else Color(0xFF79747E).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(32.dp)
                            ),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = footerInputBg
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Subcompose logo active search emblem inside container exactly like the HTML logo
                            SubcomposeAsyncImage(
                                model = viewModel.appLogoUrl,
                                contentDescription = "Active Logo Indicator",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                loading = { CircularProgressIndicator(strokeWidth = 1.dp) }
                            )

                            TextField(
                                value = inputText,
                                onValueChange = { viewModel.onInputTextChange(it) },
                                placeholder = { Text("Ask AkinAI anything...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 4,
                                singleLine = false,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Send
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        viewModel.sendMessage()
                                        keyboardController?.hide()
                                    }
                                )
                            )

                            FloatingActionButton(
                                onClick = {
                                    viewModel.sendMessage()
                                    keyboardController?.hide()
                                },
                                shape = CircleShape,
                                containerColor = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("send_button"),
                                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send Message",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Navigation Footer Tab Bar matching HTML precisely
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        HorizontalDivider(
                            color = if (isDark) Color(0xFF35343A) else Color(0xFF79747E).copy(alpha = 0.1f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chat Active Tab
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Chat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Discover Tab (Stylized placeholder)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Discover",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Discover",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }

                            // Recent Tab (Stylized placeholder)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Recent",
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Recent",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Config Key / Custom settings popup dialog
    if (showSettings) {
        SettingsDialog(
            customApiKey = customApiKey,
            onClose = { viewModel.toggleSettings(false) },
            onUpdateKey = { viewModel.onCustomApiKeyChange(it) },
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
            appLogo = viewModel.appLogoUrl
        )
    }
}

@Composable
fun SuggestionRow(
    onSelectPrompt: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "Suggested Questions:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Who made AkinAI?",
                "Tell me about Akin S. Sokpah"
            ).forEach { suggestion ->
                Card(
                    onClick = { onSelectPrompt(suggestion) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    viewModel: ChatViewModel
) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (!isUser) {
                // Inline tiny indicator Logo for AI messages
                SubcomposeAsyncImage(
                    model = viewModel.appLogoUrl,
                    contentDescription = "AkinAI Sender",
                    modifier = Modifier
                        .padding(end = 8.dp, top = 2.dp)
                        .size(24.dp)
                        .clip(CircleShape),
                    loading = { CircularProgressIndicator(strokeWidth = 1.dp) }
                )
            }

            // Message Bubble Text
            val isDarkTheme = isSystemInDarkTheme()
            val bubbleShape = if (isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 2.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            } else {
                RoundedCornerShape(topStart = 2.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            }
            Card(
                shape = bubbleShape,
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isUser -> MaterialTheme.colorScheme.primary
                        message.isError -> MaterialTheme.colorScheme.errorContainer
                        else -> if (isDarkTheme) BubbleAiDark else BubbleAiLight
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.isPending) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.6.dp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "AkinAI is composing...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = when {
                                isUser -> MaterialTheme.colorScheme.onPrimary
                                message.isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Creator Profile Card shown below the explanation when asked!
        if (message.showCreatorProfile) {
            Spacer(modifier = Modifier.height(8.dp))
            CreatorProfileCard(viewModel = viewModel)
        }
    }
}

@Composable
fun CreatorProfileCard(viewModel: ChatViewModel) {
    val isDarkTheme = isSystemInDarkTheme()
    val creatorCardBg = if (isDarkTheme) CardBackgroundDark else Color(0xFFF3EDF7)
    val creatorCardBorder = if (isDarkTheme) Color(0xFF49454F) else Color(0xFFCAC4D0)
    val creatorImageBorder = if (isDarkTheme) Color(0xFF9880E5) else Color(0xFFD0BCFF)

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(vertical = 4.dp)
            .border(1.dp, creatorCardBorder, RoundedCornerShape(topStart = 2.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = creatorCardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile image with a premium rounded frame exactly matching Lead Visionary look in HTML
            SubcomposeAsyncImage(
                model = viewModel.creatorPictureUrl,
                contentDescription = viewModel.creatorName,
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, creatorImageBorder, CircleShape),
                contentScale = ContentScale.Crop,
                loading = { Box(modifier = Modifier.fillMaxSize()) { CircularProgressIndicator() } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = viewModel.creatorName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color(0xFF1D1B20)
            )

            Text(
                text = "LEAD VISIONARY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) MaterialTheme.colorScheme.secondary else Color(0xFF49454F),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = viewModel.creatorBio,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Badge inside card representing AkinAI brand logo
                SubcomposeAsyncImage(
                    model = viewModel.appLogoUrl,
                    contentDescription = "AkinAI Badge",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )

                Text(
                    text = "Crafted with 💖 in Google AI Studio",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    customApiKey: String,
    onClose: () -> Unit,
    onUpdateKey: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    appLogo: String
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SubcomposeAsyncImage(
                    model = appLogo,
                    contentDescription = "Branding Configuration",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AkinAI Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Configure your Custom Gemini API Key or rely on the automatically configured platform key.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = onUpdateKey,
                    label = { Text("Backup Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_field"),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("CANCEL", color = MaterialTheme.colorScheme.secondary)
                    }
                    Button(onClick = onClose) {
                        Text("SAVE")
                    }
                }
            }
        }
    }
}


