package ke.co.talin.myapplication;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import ke.co.talin.myapplication.Common.Common;
import ke.co.talin.myapplication.Database.Database;
import ke.co.talin.myapplication.Interface.ItemClickListener;
import ke.co.talin.myapplication.Model.Food;
import ke.co.talin.myapplication.ViewHolder.FoodViewHolder;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class FoodList extends AppCompatActivity {


    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    FirebaseDatabase mDatabase;
    DatabaseReference foodsList;

    String categoryId ="";

    FirebaseRecyclerAdapter<Food,FoodViewHolder> mAdapter;

    //Search Functionality
    FirebaseRecyclerAdapter<Food,FoodViewHolder> searchAdapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar mSearchBar;

    //Favorites
    Database localDb;

    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
        .setDefaultFontPath("fonts/restaurant_font.otf")
        .setFontAttrId(R.attr.fontPath)
        .build());
        setContentView(R.layout.activity_food_list);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh);

        mDatabase = FirebaseDatabase.getInstance();
        foodsList = mDatabase.getReference("Foods");

        //Local DB
        localDb = new Database(this);

        //view
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //Get Intent Here
                if(getIntent() != null)
                    categoryId = getIntent().getStringExtra("categoryId");
                if(!categoryId.isEmpty())
                {
                    if(Common.isConnectedToInternet(getBaseContext()))
                        loadFoodsList(categoryId);
                    else
                    {
                        Toast.makeText(FoodList.this, "Please Check Your Connection!!..", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        });

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                //Get Intent Here
                if(getIntent() != null)
                    categoryId = getIntent().getStringExtra("categoryId");
                if(!categoryId.isEmpty())
                {
                    if(Common.isConnectedToInternet(getBaseContext()))
                        loadFoodsList(categoryId);
                    else
                    {
                        Toast.makeText(FoodList.this, "Please Check Your Connection!!..", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        });

        mRecyclerView = findViewById(R.id.foods_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);




        //Search
        mSearchBar = findViewById(R.id.searchBar);
        mSearchBar.setHint("Enter Your Food");
//        mSearchBar.setSpeechMode(false);
        loadSuggestionList();
        mSearchBar.setLastSuggestions(suggestList);
        mSearchBar.setCardViewElevation(10);
        mSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //when user types their text , we will change the suggestion list

                List<String> suggest = new ArrayList<String>();
                for(String search:suggestList) //loop in suggestion list
                {
                    if(search.toLowerCase().contains(mSearchBar.getText().toLowerCase()))
                        suggest.add(search);
                }
                mSearchBar.setLastSuggestions(suggest);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {
                //when Search bar is closed
                //return original suggest adapter
                if(!enabled)
                    mRecyclerView.setAdapter(mAdapter);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                //when search is finished
                //show result
                startSearch(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

    }

    private void startSearch(CharSequence text) {
        //Create query by name
        Query searchByName = foodsList.orderByChild("name").equalTo(text.toString());
        //Create Options with Query
        FirebaseRecyclerOptions<Food> foodOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByName,Food.class)
                .build();

        searchAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(foodOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FoodViewHolder viewHolder, int position, @NonNull Food model) {
                viewHolder.txtfood.setText(model.getName());
                Picasso.get().load(model.getImage())
                        .into(viewHolder.images);

                final Food local = model;
                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Start New Activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",searchAdapter.getRef(position).getKey()); //send food Id to new Activity
                        startActivity(foodDetail);
                    }
                });
            }

            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.food_item,viewGroup,false);
                return new FoodViewHolder(itemView);
            }
        };
        searchAdapter.startListening();
        mRecyclerView.setAdapter(searchAdapter);
    }


    private void loadSuggestionList() {
        foodsList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot snapshot: dataSnapshot.getChildren())
                        {
                            Food item = snapshot.getValue(Food.class);
                            suggestList.add(item.getName()); //Add name of foods to suggestions list

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadFoodsList(String categoryId) {
        //Create query by Category Id
        Query searchByCat = foodsList.orderByChild("menuId").equalTo(categoryId);
        //Create Options with Query
        FirebaseRecyclerOptions<Food> menuOptions = new FirebaseRecyclerOptions.Builder<Food>()
                .setQuery(searchByCat,Food.class)
                .build();

        mAdapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(menuOptions) {
            @Override
            protected void onBindViewHolder(@NonNull final FoodViewHolder holder, final int position, @NonNull final Food model) {
                holder.txtfood.setText(model.getName());
                holder.txtprice.setText(String.format("KES %s",model.getPrice().toString()));
                Picasso.get().load(model.getImage())
                        .into(holder.images);

                //Add Favorites
                if(localDb.isFavorites(mAdapter.getRef(position).getKey()))
                    holder.fave.setImageResource(R.drawable.ic_favorite_black_24dp);

                //click to change state of Favorites
                holder.fave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!localDb.isFavorites(mAdapter.getRef(position).getKey()))
                        {
                            localDb.addToFavorites(mAdapter.getRef(position).getKey());
                            holder.fave.setImageResource(R.drawable.ic_favorite_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was Added To Favorites", Toast.LENGTH_SHORT).show();

                        }
                        else
                        {
                            localDb.removeFromFavorites(mAdapter.getRef(position).getKey());
                            holder.fave.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                            Toast.makeText(FoodList.this, ""+model.getName()+"was removed from Favorites", Toast.LENGTH_SHORT).show();


                        }
                    }
                });

                final Food local = model;
                holder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Start New Activity
                        Intent foodDetail = new Intent(FoodList.this,FoodDetail.class);
                        foodDetail.putExtra("FoodId",mAdapter.getRef(position).getKey()); //send food Id to new Activity
                        startActivity(foodDetail);
                    }
                });
            }


            @NonNull
            @Override
            public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View itemView = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.food_item,viewGroup,false);
                return new FoodViewHolder(itemView);
            }
        };
        //Set Adapter
//        Log.d("TAG",""+mAdapter.getItemCount());
        mAdapter.startListening();
        mRecyclerView.setAdapter(mAdapter);
        swipeRefreshLayout.setRefreshing(false);

    }

    @Override
    protected void onStop() {
        super.onStop();
        mAdapter.stopListening();
        searchAdapter.stopListening();
    }
}
