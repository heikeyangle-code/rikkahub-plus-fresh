     1|package me.rerere.rikkahub.ui.pages.extensions
     2|
     3|import androidx.activity.compose.rememberLauncherForActivityResult
     4|import androidx.activity.result.contract.ActivityResultContracts
     5|import androidx.compose.foundation.clickable
     6|import androidx.compose.foundation.layout.Arrangement
     7|import androidx.compose.foundation.layout.Box
     8|import androidx.compose.foundation.layout.Column
     9|import androidx.compose.foundation.layout.PaddingValues
    10|import androidx.compose.foundation.layout.Row
    11|import androidx.compose.foundation.layout.Spacer
    12|import androidx.compose.foundation.layout.fillMaxSize
    13|import androidx.compose.foundation.layout.fillMaxWidth
    14|import androidx.compose.foundation.layout.height
    15|import androidx.compose.foundation.layout.padding
    16|import androidx.compose.foundation.layout.size
    17|import androidx.compose.foundation.layout.width
    18|import androidx.compose.foundation.lazy.LazyColumn
    19|import androidx.compose.foundation.lazy.items
    20|import androidx.compose.foundation.shape.RoundedCornerShape
    21|import androidx.compose.foundation.text.KeyboardOptions
    22|import androidx.compose.material3.AlertDialog
    23|import androidx.compose.material3.Card
    24|import androidx.compose.material3.CircularProgressIndicator
    25|import androidx.compose.material3.DropdownMenu
    26|import androidx.compose.material3.DropdownMenuItem
    27|import androidx.compose.material3.ExperimentalMaterial3Api
    28|import androidx.compose.material3.FloatingActionButton
    29|import androidx.compose.material3.HorizontalDivider
    30|import androidx.compose.material3.Icon
    31|import androidx.compose.material3.IconButton
    32|import androidx.compose.material3.LargeFlexibleTopAppBar
    33|import androidx.compose.material3.MaterialTheme
    34|import androidx.compose.material3.ModalBottomSheet
    35|import androidx.compose.material3.OutlinedTextField
    36|import androidx.compose.material3.Scaffold
    37|import androidx.compose.material3.SmallFloatingActionButton
    38|import androidx.compose.material3.Surface
    39|import androidx.compose.material3.Text
    40|import androidx.compose.material3.TextButton
    41|import androidx.compose.material3.TopAppBarDefaults
    42|import androidx.compose.material3.rememberModalBottomSheetState
    43|import androidx.compose.runtime.Composable
    44|import androidx.compose.runtime.getValue
    45|import androidx.compose.runtime.mutableStateOf
    46|import androidx.compose.runtime.remember
    47|import androidx.compose.runtime.saveable.rememberSaveable
    48|import androidx.compose.runtime.setValue
    49|import androidx.compose.ui.Alignment
    50|import androidx.compose.ui.Modifier
    51|import androidx.compose.ui.input.nestedscroll.nestedScroll
    52|import androidx.compose.ui.platform.LocalContext
    53|import androidx.compose.ui.res.stringResource
    54|import androidx.compose.ui.text.font.FontFamily
    55|import androidx.compose.ui.text.font.FontWeight
    56|import androidx.compose.ui.text.input.ImeAction
    57|import androidx.compose.ui.unit.dp
    58|import androidx.compose.ui.unit.sp
    59|import androidx.lifecycle.compose.collectAsStateWithLifecycle
    60|import me.rerere.rikkahub.R
    61|import me.rerere.hugeicons.HugeIcons
    62|import me.rerere.hugeicons.stroke.Add01
    63|import me.rerere.hugeicons.stroke.Delete01
    64|import me.rerere.hugeicons.stroke.Download01
    65|import me.rerere.hugeicons.stroke.MoreVertical
    66|import me.rerere.hugeicons.stroke.Puzzle
    67|import me.rerere.rikkahub.data.files.SkillFrontmatterParser
    68|import me.rerere.rikkahub.data.files.SkillMetadata
    69|import me.rerere.rikkahub.data.files.SkillRegistry
    70|import me.rerere.rikkahub.Screen
    71|import me.rerere.rikkahub.ui.components.nav.BackButton
    72|import me.rerere.rikkahub.ui.components.ui.RikkaConfirmDialog
    73|import me.rerere.rikkahub.ui.context.LocalNavController
    74|import me.rerere.rikkahub.ui.context.LocalToaster
    75|import me.rerere.rikkahub.ui.theme.CustomColors
    76|import me.rerere.rikkahub.utils.plus
    77|import org.koin.androidx.compose.koinViewModel
    78|
    79|@Composable
    80|fun SkillsPage() {
    81|    val navController = LocalNavController.current
    82|    val vm = koinViewModel<SkillsVM>()
    83|    val skills by vm.skills.collectAsStateWithLifecycle()
    84|    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    85|    val toaster = LocalToaster.current
    86|    val context = LocalContext.current
    87|    var showImportSheet by rememberSaveable { mutableStateOf(false) }
    88|    var showMarketplaceDialog by rememberSaveable { mutableStateOf(false) }
    89|    var deleteTarget by remember { mutableStateOf<SkillMetadata?>(null) }
    90|
    91|    // File picker launcher (.zip / .md)
    92|    val filePickerLauncher = rememberLauncherForActivityResult(
    93|        contract = ActivityResultContracts.GetContent()
    94|    ) { uri ->
    95|        uri?.let {
    96|            vm.importFromLocalFile(it, context) { success, msg ->
    97|                if (success) {
    98|                    toaster.show(context.getString(R.string.skills_page_install_success, msg))
    99|                } else {
   100|                    toaster.show(msg)
   101|                }
   102|            }
   103|        }
   104|    }
   105|
   106|    // Folder picker launcher
   107|    val folderPickerLauncher = rememberLauncherForActivityResult(
   108|        contract = ActivityResultContracts.OpenDocumentTree()
   109|    ) { uri ->
   110|        uri?.let {
   111|            // Take persistable permission
   112|            val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
   113|            context.contentResolver.takePersistableUriPermission(it, takeFlags)
   114|            vm.importFromFolder(it, context) { success, msg ->
   115|                if (success) {
   116|                    toaster.show(context.getString(R.string.skills_page_install_success, msg))
   117|                } else {
   118|                    toaster.show(msg)
   119|                }
   120|            }
   121|        }
   122|    }
   123|
   124|    Scaffold(
   125|        topBar = {
   126|            LargeFlexibleTopAppBar(
   127|                title = { Text(stringResource(R.string.skills_page_title)) },
   128|                navigationIcon = { BackButton() },
   129|                actions = {
   130|                    TextButton(onClick = {
   131|                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
   132|                            val uri = android.net.Uri.parse(vm.getSkillsDir().absolutePath)
   133|                            setDataAndType(uri, "resource/folder")
   134|                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
   135|                        }
   136|                        try { context.startActivity(intent) } catch (_: Exception) {}
   137|                    }) {
   138|                        Text("📂 打开目录", style = MaterialTheme.typography.labelMedium)
   139|                    }
   140|                },
   141|                scrollBehavior = scrollBehavior,
   142|                colors = CustomColors.topBarColors,
   143|            )
   144|        },
   145|        floatingActionButton = {
   146|            Column(
   147|                horizontalAlignment = Alignment.End,
   148|                verticalArrangement = Arrangement.spacedBy(12.dp),
   149|            ) {
   150|                SmallFloatingActionButton(onClick = { showMarketplaceDialog = true }) {
   151|                    Icon(
   152|                        HugeIcons.Puzzle,
   153|                        contentDescription = "从 Market 导入",
   154|                        tint = MaterialTheme.colorScheme.tertiary,
   155|                    )
   156|                }
   157|                FloatingActionButton(
   158|                    onClick = { showImportSheet = true },
   159|                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
   160|                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
   161|                    modifier = Modifier.size(64.dp),
   162|                ) {
   163|                    Column(
   164|                        horizontalAlignment = Alignment.CenterHorizontally,
   165|                        verticalArrangement = Arrangement.Center,
   166|                    ) {
   167|                        Icon(
   168|                            HugeIcons.Add01,
   169|                            contentDescription = null,
   170|                            modifier = Modifier.size(24.dp),
   171|                        )
   172|                        Text(
   173|                            "导入",
   174|                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
   175|                            fontWeight = FontWeight.Medium,
   176|                        )
   177|                    }
   178|                }
   179|            }
   180|        },
   181|        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
   182|        containerColor = CustomColors.topBarColors.containerColor,
   183|    ) { innerPadding ->
   184|        LazyColumn(
   185|            modifier = Modifier.fillMaxSize(),
   186|            contentPadding = innerPadding + PaddingValues(16.dp),
   187|            verticalArrangement = Arrangement.spacedBy(12.dp),
   188|        ) {
   189|            if (skills.isEmpty()) {
   190|                item {
   191|                    Column(
   192|                        modifier = Modifier
   193|                            .fillMaxWidth()
   194|                            .padding(vertical = 48.dp),
   195|                        horizontalAlignment = Alignment.CenterHorizontally,
   196|                        verticalArrangement = Arrangement.spacedBy(12.dp),
   197|                    ) {
   198|                        Icon(
   199|                            imageVector = HugeIcons.Puzzle,
   200|                            contentDescription = null,
   201|                            modifier = Modifier.size(48.dp),
   202|                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
   203|                        )
   204|                        Text(
   205|                            text = stringResource(R.string.skills_page_empty_title),
   206|                            style = MaterialTheme.typography.bodyLarge,
   207|                            color = MaterialTheme.colorScheme.onSurfaceVariant,
   208|                        )
   209|                        Text(
   210|                            text = stringResource(R.string.skills_page_empty_hint),
   211|                            style = MaterialTheme.typography.bodySmall,
   212|                            color = MaterialTheme.colorScheme.onSurfaceVariant,
   213|                        )
   214|                    }
   215|                }
   216|            }
   217|
   218|            // Skill 市场
   219|            item {
   220|                Text(
   221|                    "🧩 Skill 市场",
   222|                    style = MaterialTheme.typography.titleSmall,
   223|                    modifier = Modifier.padding(top = 8.dp),
   224|                )
   225|                Text(
   226|                    "点击即可安装",
   227|                    style = MaterialTheme.typography.labelSmall,
   228|                    color = MaterialTheme.colorScheme.onSurfaceVariant,
   229|                )
   230|            }
   231|            SkillRegistry.byCategory().forEach { (category, entries) ->
   232|                item {
   233|                    Text(
   234|                        category,
   235|                        style = MaterialTheme.typography.labelMedium,
   236|                        color = MaterialTheme.colorScheme.primary,
   237|                        modifier = Modifier.padding(top = 4.dp),
   238|                    )
   239|                }
   240|                entries.forEach { entry ->
   241|                    item {
   242|                        RegistrySkillCard(
   243|                            entry = entry,
   244|                            onInstall = {
   245|                                vm.installFromRegistry(entry) { success, msg ->
   246|                                    if (success) toaster.show("已安装: $msg")
   247|                                    else toaster.show("失败: $msg")
   248|                                }
   249|                            },
   250|                        )
   251|                    }
   252|                }
   253|            }
   254|
   255|            item {
   256|                Text(
   257|                    "📦 已安装",
   258|                    style = MaterialTheme.typography.titleSmall,
   259|                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
   260|                )
   261|            }
   262|
   263|            items(skills, key = { it.name }) { skill ->
   264|                SkillCard(
   265|                    skill = skill,
   266|                    onClick = { navController.navigate(Screen.SkillDetail(skill.name)) },
   267|                    onDelete = { deleteTarget = skill },
   268|                )
   269|            }
   270|        }
   271|    }
   272|
   273|    if (showImportSheet) {
   274|        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
   275|        ModalBottomSheet(
   276|            onDismissRequest = { showImportSheet = false },
   277|            sheetState = sheetState,
   278|            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
   279|        ) {
   280|            Column(
   281|                modifier = Modifier
   282|                    .fillMaxWidth()
   283|                    .padding(horizontal = 24.dp)
   284|                    .padding(bottom = 32.dp),
   285|                verticalArrangement = Arrangement.spacedBy(4.dp),
   286|            ) {
   287|                Text(
   288|                    text = stringResource(R.string.skills_page_import_skill_title),
   289|                    style = MaterialTheme.typography.titleMedium,
   290|                    fontWeight = FontWeight.Bold,
   291|                    modifier = Modifier.padding(bottom = 16.dp),
   292|                )
   293|
   294|                // Option 1: Import file
   295|                Surface(
   296|                    onClick = {
   297|                        showImportSheet = false
   298|                        filePickerLauncher.launch("*/*")
   299|                    },
   300|                    shape = RoundedCornerShape(12.dp),
   301|                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
   302|                    modifier = Modifier.fillMaxWidth(),
   303|                ) {
   304|                    Row(
   305|                        modifier = Modifier
   306|                            .fillMaxWidth()
   307|                            .padding(16.dp),
   308|                        verticalAlignment = Alignment.CenterVertically,
   309|                    ) {
   310|                        Icon(
   311|                            HugeIcons.Download01,
   312|                            contentDescription = null,
   313|                            modifier = Modifier.size(28.dp),
   314|                            tint = MaterialTheme.colorScheme.primary,
   315|                        )
   316|                        Spacer(Modifier.width(16.dp))
   317|                        Column(modifier = Modifier.weight(1f)) {
   318|                            Text(
   319|                                stringResource(R.string.skills_page_import_file),
   320|                                style = MaterialTheme.typography.bodyLarge,
   321|                                fontWeight = FontWeight.Medium,
   322|                            )
   323|                            Text(
   324|                                "支持 .zip 和 .md 文件",
   325|                                style = MaterialTheme.typography.bodySmall,
   326|                                color = MaterialTheme.colorScheme.onSurfaceVariant,
   327|                            )
   328|                        }
   329|                        Icon(
   330|                            HugeIcons.ArrowRight01,
   331|                            contentDescription = null,
   332|                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
   333|                        )
   334|                    }
   335|                }
   336|
   337|                Spacer(Modifier.height(4.dp))
   338|
   339|                // Option 2: Import folder
   340|                Surface(
   341|                    onClick = {
   342|                        showImportSheet = false
   343|                        folderPickerLauncher.launch(null)
   344|                    },
   345|                    shape = RoundedCornerShape(12.dp),
   346|                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
   347|                    modifier = Modifier.fillMaxWidth(),
   348|                ) {
   349|                    Row(
   350|                        modifier = Modifier
   351|                            .fillMaxWidth()
   352|                            .padding(16.dp),
   353|                        verticalAlignment = Alignment.CenterVertically,
   354|                    ) {
   355|                        Icon(
   356|                            HugeIcons.Book01,
   357|                            contentDescription = null,
   358|                            modifier = Modifier.size(28.dp),
   359|                            tint = MaterialTheme.colorScheme.primary,
   360|                        )
   361|                        Spacer(Modifier.width(16.dp))
   362|                        Column(modifier = Modifier.weight(1f)) {
   363|                            Text(
   364|                                stringResource(R.string.skills_page_import_folder),
   365|                                style = MaterialTheme.typography.bodyLarge,
   366|                                fontWeight = FontWeight.Medium,
   367|                            )
   368|                            Text(
   369|                                "选择包含 SKILL.md 的文件夹",
   370|                                style = MaterialTheme.typography.bodySmall,
   371|                                color = MaterialTheme.colorScheme.onSurfaceVariant,
   372|                            )
   373|                        }
   374|                        Icon(
   375|                            HugeIcons.ArrowRight01,
   376|                            contentDescription = null,
   377|                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
   378|                        )
   379|                    }
   380|                }
   381|            }
   382|        }
   383|    }
   384|
   385|    if (showMarketplaceDialog) {
   386|        MarketplaceDialog(
   387|            onDismiss = { showMarketplaceDialog = false },
   388|            onConfirm = { url ->
   389|                vm.fetchRemoteMarketplace(url) { success, message ->
   390|                    showMarketplaceDialog = false
   391|                    toaster.show(if (success) message else "失败: $message")
   392|                }
   393|            },
   394|        )
   395|    }
   396|
   397|    RikkaConfirmDialog(
   398|        show = deleteTarget != null,
   399|        title = stringResource(R.string.skills_page_delete_title),
   400|        confirmText = stringResource(R.string.delete),
   401|        dismissText = stringResource(R.string.cancel),
   402|        onConfirm = {
   403|            deleteTarget?.let { vm.deleteSkill(it.name) }
   404|            deleteTarget = null
   405|        },
   406|        onDismiss = { deleteTarget = null },
   407|    ) {
   408|        Text(stringResource(R.string.skills_page_delete_message, deleteTarget?.name ?: ""))
   409|    }
   410|}
   411|
   412|@Composable
   413|private fun RegistrySkillCard(
   414|    entry: SkillRegistry.RegistryEntry,
   415|    onInstall: () -> Unit,
   416|) {
   417|    Card(
   418|        onClick = onInstall,
   419|        modifier = Modifier.fillMaxWidth(),
   420|        colors = CardDefaults.cardColors(
   421|            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
   422|        ),
   423|    ) {
   424|        Row(
   425|            modifier = Modifier
   426|                .fillMaxWidth()
   427|                .padding(12.dp),
   428|            verticalAlignment = Alignment.CenterVertically,
   429|        ) {
   430|            Column(modifier = Modifier.weight(1f)) {
   431|                Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
   432|                Text(entry.description, style = MaterialTheme.typography.bodySmall,
   433|                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
   434|                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   435|                    Text(entry.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
   436|                    Text("· ${entry.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   437|                }
   438|            }
   439|            TextButton(onClick = onInstall) { Text("安装") }
   440|        }
   441|    }
   442|}
   443|
   444|@Composable
   445|private fun SkillCard(
   446|    skill: SkillMetadata,
   447|    onClick: () -> Unit,
   448|    onDelete: () -> Unit,
   449|) {
   450|    var menuExpanded by remember { mutableStateOf(false) }
   451|
   452|    Card(
   453|        onClick = onClick,
   454|        modifier = Modifier.fillMaxWidth(),
   455|        colors = CustomColors.cardColorsOnSurfaceContainer,
   456|    ) {
   457|        Row(
   458|            modifier = Modifier
   459|                .fillMaxWidth()
   460|                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
   461|            verticalAlignment = Alignment.CenterVertically,
   462|        ) {
   463|            Icon(
   464|                imageVector = HugeIcons.Puzzle,
   465|                contentDescription = null,
   466|                modifier = Modifier.size(20.dp),
   467|                tint = MaterialTheme.colorScheme.primary,
   468|            )
   469|            Column(
   470|                modifier = Modifier
   471|                    .weight(1f)
   472|                    .padding(start = 12.dp),
   473|                verticalArrangement = Arrangement.spacedBy(2.dp),
   474|            ) {
   475|                Row(verticalAlignment = Alignment.CenterVertically) {
   476|                    Text(
   477|                        text = skill.name,
   478|                        style = MaterialTheme.typography.titleSmallEmphasized,
   479|                    )
   480|                    if (skill.version != null) {
   481|                        Spacer(Modifier.width(6.dp))
   482|                        Surface(
   483|                            shape = RoundedCornerShape(4.dp),
   484|                            color = MaterialTheme.colorScheme.secondaryContainer,
   485|                        ) {
   486|                            Text(
   487|                                "v${skill.version}",
   488|                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
   489|                                style = MaterialTheme.typography.labelSmall,
   490|                                color = MaterialTheme.colorScheme.onSecondaryContainer,
   491|                            )
   492|                        }
   493|                    }
   494|                }
   495|                Text(
   496|                    text = skill.description,
   497|                    style = MaterialTheme.typography.bodySmall,
   498|                    color = MaterialTheme.colorScheme.onSurfaceVariant,
   499|                    maxLines = 2,
   500|                )
   501|                Row(
   502|                    horizontalArrangement = Arrangement.spacedBy(8.dp),
   503|                    verticalAlignment = Alignment.CenterVertically,
   504|                ) {
   505|                    skill.category?.let { cat ->
   506|                        Text(cat, style = MaterialTheme.typography.labelSmall,
   507|                            color = MaterialTheme.colorScheme.primary)
   508|                    }
   509|                    if (skill.triggers.isNotEmpty()) {
   510|                        Text(
   511|                            "触发: ${skill.triggers.take(3).joinToString()}",
   512|                            style = MaterialTheme.typography.labelSmall,
   513|                            color = MaterialTheme.colorScheme.tertiary,
   514|                            maxLines = 1,
   515|                        )
   516|                    }
   517|                    val fileCount = skill.linkedFiles.values.sumOf { it.size }
   518|                    if (fileCount > 0) {
   519|                        Text(
   520|                            "📎$fileCount",
   521|                            style = MaterialTheme.typography.labelSmall,
   522|                            color = MaterialTheme.colorScheme.onSurfaceVariant,
   523|                        )
   524|                    }
   525|                }
   526|                if (!skill.compatibility.isNullOrBlank()) {
   527|                    Text(
   528|                        text = skill.compatibility,
   529|                        style = MaterialTheme.typography.labelSmall,
   530|                        color = MaterialTheme.colorScheme.tertiary,
   531|                    )
   532|                }
   533|            }
   534|            Box {
   535|                IconButton(onClick = { menuExpanded = true }) {
   536|                    Icon(
   537|                        imageVector = HugeIcons.MoreVertical,
   538|                        contentDescription = "更多",
   539|                    )
   540|                }
   541|                DropdownMenu(
   542|                    expanded = menuExpanded,
   543|                    onDismissRequest = { menuExpanded = false },
   544|                ) {
   545|                    DropdownMenuItem(
   546|                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
   547|                        leadingIcon = {
   548|                            Icon(HugeIcons.Delete01, null, tint = MaterialTheme.colorScheme.error)
   549|                        },
   550|                        onClick = {
   551|                            menuExpanded = false
   552|                            onDelete()
   553|                        },
   554|                    )
   555|                }
   556|            }
   557|        }
   558|    }
   559|
        }
    }
}