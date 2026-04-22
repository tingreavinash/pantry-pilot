package com.pantrypilot.ui.members;

import androidx.compose.foundation.layout.Arrangement;
import androidx.compose.foundation.layout.PaddingValues;
import androidx.compose.material.icons.Icons;
import androidx.compose.material3.MaterialTheme;
import androidx.compose.runtime.Composable;
import androidx.compose.runtime.MutableState;
import androidx.compose.runtime.State;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.pantrypilot.data.firebase.MemberRepository;
import com.pantrypilot.data.firebase.ShoppingRepository;
import com.pantrypilot.data.model.Member;
import com.pantrypilot.data.model.ShoppingItem;
import com.pantrypilot.ui.common.Components;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

// ─── ViewModel ────────────────────────────────────────────────────────────────
@HiltViewModel
class MembersViewModel extends ViewModel {

    public final MutableLiveData<List<Member>> members = new MutableLiveData<>(List.of());
    public final MutableLiveData<List<ShoppingItem>> shoppingItems = new MutableLiveData<>(List.of());
    private final MemberRepository memberRepo;
    private final ShoppingRepository shoppingRepo;
    private final FirebaseAuth auth;

    @Inject
    MembersViewModel(MemberRepository mr, ShoppingRepository sr, FirebaseAuth auth) {
        this.memberRepo = mr;
        this.shoppingRepo = sr;
        this.auth = auth;
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        if (!uid.isEmpty()) {
            memberRepo.subscribeMembers(uid, members);
            shoppingRepo.subscribeShoppingItems(uid, shoppingItems);
        }
    }

    public void addMember(Member member) {
        memberRepo.addMember(auth.getCurrentUser().getUid(), member);
    }

    public void deleteMember(Member member) {
        memberRepo.deleteMember(auth.getCurrentUser().getUid(), member.id);
    }

    public int assignedCount(String memberName) {
        List<ShoppingItem> items = shoppingItems.getValue();
        if (items == null) return 0;
        return (int) items.stream().filter(i -> memberName.equals(i.assignedTo) && !i.bought).count();
    }

    public List<ShoppingItem> assignedItems(String memberName) {
        List<ShoppingItem> items = shoppingItems.getValue();
        if (items == null) return List.of();
        return items.stream().filter(i -> memberName.equals(i.assignedTo)).collect(Collectors.toList());
    }

    @Override
    protected void onCleared() {
        memberRepo.removeListener();
        shoppingRepo.removeListener();
        super.onCleared();
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────
public class MembersScreen {

    private static final List<String> EMOJI_LIST = List.of(
            "👨", "👩", "👦", "👧", "👶", "👴", "👵", "🧑", "👱", "👮", "🧑‍💻", "👩‍🍳",
            "🧑‍🌾", "👩‍🔬", "🧑‍🎨", "👩‍💼", "🧑‍🚀", "🐱", "🐶", "🦁", "🐯", "🐻", "🦊", "🐼",
            "🐨", "🦄", "🐸", "🐙", "🦋", "🌟"
    );

    @Composable
    public static void MembersScreen() {
        MembersViewModel vm = hiltViewModel();
        State<List<Member>> members = vm.members.observeAsState(List.of());

        MutableState<Boolean> showAddSheet = remember {
            mutableStateOf(false)
        }
        MutableState<Member> selectedMember = remember {
            mutableStateOf(null)
        }

        Scaffold(
                topBar = {TopAppBar(title = {Text("Members")}); },
        floatingActionButton = {
                FloatingActionButton(
                        onClick = () -> showAddSheet.setValue(true),
                        containerColor = MaterialTheme.colorScheme.secondary
                ){Icon(Icons.Filled.PersonAdd, "Add member"); }
                }
        ){
            padding ->
            if (members.getValue().isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Components.EmptyState("👥", "No members yet",
                            "Add member", () -> showAddSheet.setValue(true));
                }
            } else {
                LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    items(members.getValue()) {
                        member ->
                                MemberCard(
                                        member = member,
                                        assignedCount = vm.assignedCount(member.name),
                                        onTap = () -> selectedMember.setValue(member),
                                        onDelete = () -> vm.deleteMember(member)
                                );
                    }
                }
            }
        }

        if (showAddSheet.getValue()) {
            AddMemberSheet(
                    onDismiss = () -> showAddSheet.setValue(false),
                    onSave = m -> {
                        vm.addMember(m);
                        showAddSheet.setValue(false);
                    }
            );
        }

        if (selectedMember.getValue() != null) {
            MemberDetailSheet(
                    member = selectedMember.getValue(),
                    items = vm.assignedItems(selectedMember.getValue().name),
                    onDismiss = () -> selectedMember.setValue(null)
            );
        }
    }

    @Composable
    private static void MemberCard(Member member, int assignedCount,
                                   Runnable onTap, Runnable onDelete) {
        Card(onClick = onTap::run, modifier = Modifier.fillMaxWidth().aspectRatio(0.9f)) {
            Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(member.avatarEmoji, fontSize = 44.sp);
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(member.name, style = MaterialTheme.typography.titleMedium);
                    Text(assignedCount + " items", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant);
                }
                IconButton(onClick = onDelete::run, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, "Delete", modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error);
                }
            }
        }
    }

    @Composable
    private static void AddMemberSheet(Runnable onDismiss,
                                       java.util.function.Consumer<Member> onSave) {
        MutableState<String> name = remember {
            mutableStateOf("")
        }
        MutableState<String> emoji = remember {
            mutableStateOf("👤")
        }

        ModalBottomSheet(onDismissRequest = onDismiss::run,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Add Member", style = MaterialTheme.typography.titleLarge);
                Spacer(Modifier.height(16.dp));
                OutlinedTextField(value = name.getValue(), onValueChange = v -> name.setValue(v),
                        label = {Text("Name")}, modifier = Modifier.fillMaxWidth(), singleLine = true);
                Spacer(Modifier.height(12.dp));
                Text("Choose avatar", style = MaterialTheme.typography.labelMedium);
                Spacer(Modifier.height(6.dp));
                LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.height(160.dp)
                ) {
                    items(EMOJI_LIST) {
                        e ->
                                Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .then( if (e.equals(emoji.getValue()))
                            Modifier.background(MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(8.dp))
                        else Modifier)
                                    .clickable(() -> emoji.setValue(e))
                        ){
                            Text(e, fontSize = 22.sp);
                        }
                    }
                }
                Spacer(Modifier.height(20.dp));
                Button(onClick = () -> {
                    Member m = new Member();
                    m.name = name.getValue();
                    m.avatarEmoji = emoji.getValue();
                    onSave.accept(m);
                }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Add Member");
                }
                Spacer(Modifier.height(32.dp));
            }
        }
    }

    @Composable
    private static void MemberDetailSheet(Member member, List<ShoppingItem> items,
                                          Runnable onDismiss) {
        ModalBottomSheet(onDismissRequest = onDismiss::run) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(member.avatarEmoji, fontSize = 36.sp);
                    Text(member.name + "'s assignments", style = MaterialTheme.typography.titleMedium);
                }
                Spacer(Modifier.height(12.dp));
                if (items.isEmpty()) {
                    Text("No items assigned", color = MaterialTheme.colorScheme.onSurfaceVariant);
                } else {
                    items.forEach(item -> {
                        ListItem(
                                headlineContent = {Text(item.name)},
                                trailingContent = {
                                        Text(item.bought ? "✅" : "○", fontSize = 18.sp);
                                }
                        )
                    });
                }
                Spacer(Modifier.height(24.dp));
            }
        }
    }
}
