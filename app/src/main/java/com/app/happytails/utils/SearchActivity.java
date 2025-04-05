package com.app.happytails.utils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.SearchDogAdapter;
import com.app.happytails.utils.Adapters.SearchUserAdapter;
import com.app.happytails.utils.model.DogSearchModel;
import com.app.happytails.utils.model.UserSearchModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SearchActivity extends AppCompatActivity {

    private ImageButton backBtn;
    private LinearLayout searchToolbar;
    private RecyclerView resultsRecyclerView;
    private EditText searchInput;
    private SearchUserAdapter userAdapter;
    private SearchDogAdapter dogAdapter;

    private boolean isSearchingPeople = true;
    private Timer searchDebounceTimer;
    private static final int SEARCH_DEBOUNCE_DELAY = 300;
    private static final int MIN_SEARCH_LENGTH = 2;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private Button btnSearchPerson;
    private Button btnSearchDog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupClickListeners();
        setupSearchInputListener();
        fetchFirestoreData();
    }

    private void initializeViews() {
        backBtn = findViewById(R.id.back_btn);
        searchToolbar = findViewById(R.id.search_toolbar);
        resultsRecyclerView = findViewById(R.id.search_user_recycler_view);
        searchInput = findViewById(R.id.search_username_input);

        btnSearchPerson = findViewById(R.id.btn_search_person);
        btnSearchDog = findViewById(R.id.btn_search_dog);

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new SearchUserAdapter(getUserSearchOptions(""), this, getSupportFragmentManager());
        dogAdapter = new SearchDogAdapter(getDogSearchOptions(""), this, getSupportFragmentManager());
        resultsRecyclerView.setAdapter(userAdapter);
    }

    private void setupClickListeners() {
        backBtn.setOnClickListener(v -> onBackPressed());

        btnSearchPerson.setOnClickListener(v -> {
            isSearchingPeople = true;
            updateSearchModeUI(btnSearchPerson, btnSearchDog);
            reinitializeAdapter("");
            fetchFirestoreData();
            searchInput.setText("");
        });

        btnSearchDog.setOnClickListener(v -> {
            isSearchingPeople = false;
            updateSearchModeUI(btnSearchDog, btnSearchPerson);
            reinitializeAdapter("");
            fetchFirestoreData();
            searchInput.setText("");
        });
    }

    private void setupSearchInputListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                debounceSearch(s.toString());
                Log.d("Search", "isPerson" + isSearchingPeople);
            }
        });
    }

    @SuppressLint("ResourceAsColor")
    private void updateSearchModeUI(Button activeButton, Button inactiveButton) {
        searchInput.setHint(isSearchingPeople ? "Search people..." : "Search dogs...");
        activeButton.setTextColor(getResources().getColor(R.color.primary_color));
        inactiveButton.setTextColor(getResources().getColor(R.color.gray));
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

    private void performSearch(String queryStr) {
        queryStr = queryStr.toLowerCase();
        reinitializeAdapter(queryStr);
    }

    private void fetchFirestoreData() {
        if (!searchInput.getText().toString().isEmpty()) {
            if (isSearchingPeople) {
                db.collection("users").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<UserSearchModel> firestoreData = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            UserSearchModel model = new UserSearchModel();
                            model.setUsername(doc.getString("username"));
                            model.setUserId(doc.getString("userId"));
                            firestoreData.add(model);
                        }
                        updateUserSearchResults(firestoreData);
                    }
                });
            } else {
                db.collection("dogs").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DogSearchModel> firestoreData = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            DogSearchModel model = new DogSearchModel();
                            model.setDogName(doc.getString("dogName"));
                            model.setDogId(doc.getString("dogId"));
                            firestoreData.add(model);
                        }
                        updateDogSearchResults(firestoreData);
                    }
                });
            }
        } else {
            clearSearchResults();
        }
    }

    private void updateUserSearchResults(List<UserSearchModel> results) {
        userAdapter.updateData(results);
        resultsRecyclerView.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateDogSearchResults(List<DogSearchModel> results) {
        dogAdapter.updateData(results);
        resultsRecyclerView.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void clearSearchResults() {
        if (isSearchingPeople) {
            userAdapter.updateData(new ArrayList<>());
        } else {
            dogAdapter.updateData(new ArrayList<>());
        }
        resultsRecyclerView.setVisibility(View.GONE);
    }

    private void reinitializeAdapter(String queryStr) {
        // Re-initialize the adapter based on the search mode
        if (isSearchingPeople) {
            userAdapter = new SearchUserAdapter(getUserSearchOptions(queryStr), this, getSupportFragmentManager());
            resultsRecyclerView.setAdapter(userAdapter);
            userAdapter.startListening();
        } else {
            dogAdapter = new SearchDogAdapter(getDogSearchOptions(queryStr), this, getSupportFragmentManager());
            resultsRecyclerView.setAdapter(dogAdapter);
            dogAdapter.startListening();
        }
    }

    private FirestoreRecyclerOptions<UserSearchModel> getUserSearchOptions(String queryStr) {
        Query query = db.collection("users")
                .orderBy("username_lower")
                .startAt(queryStr)
                .endAt(queryStr + "\uf8ff");

        return new FirestoreRecyclerOptions.Builder<UserSearchModel>()
                .setQuery(query, UserSearchModel.class)
                .build();
    }

    private FirestoreRecyclerOptions<DogSearchModel> getDogSearchOptions(String queryStr) {
        Query query = db.collection("dogs")
                .orderBy("dog_lower")
                .startAt(queryStr)
                .endAt(queryStr + "\uf8ff");

        return new FirestoreRecyclerOptions.Builder<DogSearchModel>()
                .setQuery(query, DogSearchModel.class)
                .build();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (userAdapter != null) {
            userAdapter.stopListening();
        }
        if (dogAdapter != null) {
            dogAdapter.stopListening();
        }
    }
}