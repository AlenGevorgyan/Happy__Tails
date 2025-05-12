package com.app.happytails.utils;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.SearchUserAdapter;
import com.app.happytails.utils.Adapters.SearchDogAdapter;
import com.app.happytails.utils.Fragments.ProfileFragment.OnFragmentInteractionListener;
import com.app.happytails.utils.model.UserSearchModel;
import com.app.happytails.utils.model.DogSearchModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SearchActivity extends AppCompatActivity implements OnFragmentInteractionListener {

    private ImageButton backBtn;
    private RecyclerView resultsRecyclerView;
    private EditText searchInput;
    private Button userButton, dogButton;

    private SearchUserAdapter userAdapter;
    private SearchDogAdapter dogAdapter;
    private boolean isUserSearchMode = true;

    private Timer searchDebounceTimer;
    private static final int SEARCH_DEBOUNCE_DELAY = 300;
    private static final int MIN_SEARCH_LENGTH = 2;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initializeViews();
        setupSearchModeToggle();
        setupSearchInputListener();
    }

    private void initializeViews() {
        backBtn = findViewById(R.id.back_btn);
        resultsRecyclerView = findViewById(R.id.search_user_recycler_view);
        searchInput = findViewById(R.id.search_user_input);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        backBtn.setOnClickListener(v -> onBackPressed());
        userButton = findViewById(R.id.userSearch);
        dogButton = findViewById(R.id.dogSearch);

        switchToUserSearch();
    }

    private void setupSearchModeToggle() {
        userButton.setOnClickListener(v -> {
            if (!isUserSearchMode) {
                switchToUserSearch();
                performSearch(searchInput.getText().toString()); // keep query on switch
            }
        });

        dogButton.setOnClickListener(v -> {
            if (isUserSearchMode) {
                switchToDogSearch();
                performSearch(searchInput.getText().toString()); // keep query on switch
            }
        });
    }

    private void switchToUserSearch() {
        isUserSearchMode = true;
        searchInput.setHint("Search users...");
        updateButtonStates(true);
        initializeUserAdapter(searchInput.getText().toString());
    }

    private void switchToDogSearch() {
        isUserSearchMode = false;
        searchInput.setHint("Search dogs...");
        updateButtonStates(false);
        initializeDogAdapter(searchInput.getText().toString());
    }

    private void updateButtonStates(boolean userSelected) {
        // Assuming you are using the colors from the refined palette
        if (userSelected) {
            userButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accent_color)); // Use accent for selected
            userButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            dogButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white)); // Use white for unselected
            dogButton.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); // Use text_primary for unselected text
        } else {
            dogButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.accent_color)); // Use accent for selected
            dogButton.setTextColor(ContextCompat.getColor(this, R.color.white));
            userButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white)); // Use white for unselected
            userButton.setTextColor(ContextCompat.getColor(this, R.color.text_primary)); // Use text_primary for unselected text
        }
    }

    private void setupSearchInputListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                debounceSearch(s.toString());
            }
        });
    }

    private void debounceSearch(String query) {
        if (searchDebounceTimer != null) {
            searchDebounceTimer.cancel();
        }

        searchDebounceTimer = new Timer();
        searchDebounceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (query.length() >= MIN_SEARCH_LENGTH) {
                        performSearch(query);
                    } else {
                        clearSearchResults();
                    }
                });
            }
        }, SEARCH_DEBOUNCE_DELAY);
    }

    private void performSearch(String query) {
        if (isUserSearchMode) {
            initializeUserAdapter(query);
        } else {
            initializeDogAdapter(query);
        }
    }

    private void clearSearchResults() {
        if (isUserSearchMode && userAdapter != null) {
            userAdapter.updateData(new ArrayList<>());
        } else if (!isUserSearchMode && dogAdapter != null) {
            dogAdapter.updateData(new ArrayList<>());
        }
    }

    private void initializeUserAdapter(String query) {
        Query dbQuery = db.collection("users")
                .orderBy("username_lower")
                .startAt(query.toLowerCase())
                .endAt(query.toLowerCase() + "\uf8ff");

        FirestoreRecyclerOptions<UserSearchModel> options =
                new FirestoreRecyclerOptions.Builder<UserSearchModel>()
                        .setQuery(dbQuery, UserSearchModel.class)
                        .build();

        if (userAdapter != null) {
            userAdapter.stopListening();
        }

        userAdapter = new SearchUserAdapter(options, this, getSupportFragmentManager());
        resultsRecyclerView.setAdapter(userAdapter);
        userAdapter.startListening();
    }

    private void initializeDogAdapter(String query) {
        Query dbQuery = db.collection("dogs")
                .orderBy("dog_lower")
                .startAt(query.toLowerCase())
                .endAt(query.toLowerCase() + "\uf8ff");

        FirestoreRecyclerOptions<DogSearchModel> options =
                new FirestoreRecyclerOptions.Builder<DogSearchModel>()
                        .setQuery(dbQuery, DogSearchModel.class)
                        .build();

        if (dogAdapter != null) {
            dogAdapter.stopListening();
        }

        dogAdapter = new SearchDogAdapter(options, this, getSupportFragmentManager());
        resultsRecyclerView.setAdapter(dogAdapter);
        dogAdapter.startListening();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isUserSearchMode && userAdapter != null) {
            userAdapter.startListening();
        } else if (!isUserSearchMode && dogAdapter != null) {
            dogAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userAdapter != null) userAdapter.stopListening();
        if (dogAdapter != null) dogAdapter.stopListening();
    }

    @Override
    public void onProfileFragmentClosed() {
        // Optional: handle fragment closure
    }
}