package com.app.happytails.utils;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.SearchUserAdapter;
import com.app.happytails.utils.Fragments.ProfileFragment.OnFragmentInteractionListener;
import com.app.happytails.utils.model.UserSearchModel;
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
    private SearchUserAdapter userAdapter;

    private Timer searchDebounceTimer;
    private static final int SEARCH_DEBOUNCE_DELAY = 300;
    private static final int MIN_SEARCH_LENGTH = 2;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initializeViews();
        setupSearchInputListener();
    }

    private void initializeViews() {
        backBtn = findViewById(R.id.back_btn);
        resultsRecyclerView = findViewById(R.id.search_user_recycler_view);
        searchInput = findViewById(R.id.search_username_input);

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new SearchUserAdapter(getUserSearchOptions(""), this, getSupportFragmentManager());
        resultsRecyclerView.setAdapter(userAdapter);

        backBtn.setOnClickListener(v -> onBackPressed());
    }

    private void setupSearchInputListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
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

    private void performSearch(String queryStr) {
        queryStr = queryStr.toLowerCase();
        reinitializeAdapter(queryStr);
    }

    private void clearSearchResults() {
        userAdapter.updateData(new ArrayList<>());
        resultsRecyclerView.setVisibility(View.GONE);
    }

    private void reinitializeAdapter(String queryStr) {
        userAdapter = new SearchUserAdapter(getUserSearchOptions(queryStr), this, getSupportFragmentManager());
        resultsRecyclerView.setAdapter(userAdapter);
        userAdapter.startListening();
        resultsRecyclerView.setVisibility(View.VISIBLE);
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

    @Override
    protected void onStop() {
        super.onStop();
        if (userAdapter != null) {
            userAdapter.stopListening();
        }
    }

    @Override
    public void onProfileFragmentClosed() {

    }
}