package com.hackz.xyzreader.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.widget.SwipeRefreshLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.hackz.xyzreader.R;
import com.hackz.xyzreader.data.ArticleLoader;
import com.hackz.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ArticleListActivity.class.toString();
    private static final String EXTRA_ID = "article selected extra id";
    private static final String RV_POSITION ="rv position";
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private int rv_position;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private BroadcastReceiver mRefreshingReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        toolbar = findViewById(R.id.toolbar);
        emptyView = findViewById(R.id.empty_view);
        setSupportActionBar(toolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        }
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        recyclerView = findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(0, null, this);
        mRefreshingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                    swipeRefreshLayout.setRefreshing(false);

                }
            }
        };

        if (savedInstanceState == null) {
            refresh();
        } else {
            rv_position = savedInstanceState.getInt(RV_POSITION);
        }
    }

    private void refresh() {
        swipeRefreshLayout.setRefreshing(true);
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return ArticleLoader.newAllArticlesInstance(this);

    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor.getCount() > 0) {
            emptyView.setVisibility(View.GONE);
        } else {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            assert cm != null;
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni == null || !ni.isConnected()) {
                emptyView.setVisibility(View.VISIBLE);
            }
        }
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(sglm);
        recyclerView.scrollToPosition(rv_position);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        recyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor cursor;
        private ConstraintSet constraintSet;
        private final int DEFAULT_HEIGHT = 1;

        Adapter(Cursor cursor) {
            this.cursor = cursor;
            constraintSet = new ConstraintSet();
        }

        @Override
        public long getItemId(int position) {
            cursor.moveToPosition(position);
            return cursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ArticleListActivity.this, ArticleDetailActivity.class);
                    long id = getItemId(vh.getAdapterPosition());
                    intent.putExtra(EXTRA_ID, id);
                    startActivity(intent);

                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = cursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's dateTextView");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            cursor.moveToPosition(position);
            holder.titleView.setText(cursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            String author = cursor.getString(ArticleLoader.Query.AUTHOR);
            holder.authorTextView.setText(String.format(getString(R.string.by_author), author));
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.dateTextView.setText(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString());
            } else {
                holder.dateTextView.setText(
                        outputFormat.format(publishedDate));
            }
            String url = cursor.getString(ArticleLoader.Query.THUMB_URL);
            //aspect ratio as single number, as from query
            float aspectRatio = cursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);

            //aspect ratio as proportion width:height
            String ratio = String.format(Locale.ENGLISH, "%.2f:%d", aspectRatio, DEFAULT_HEIGHT);
            constraintSet.clone(holder.constraintLayout);
            constraintSet.setDimensionRatio(holder.thumbnailView.getId(), ratio);
            constraintSet.applyTo(holder.constraintLayout);
            Picasso.get().load(url).placeholder(R.drawable.books_placeholder_coffee)
                    .error(R.drawable.books_placeholder_coffee).into(holder.thumbnailView);

        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailView;
        TextView titleView;
        TextView dateTextView;
        TextView authorTextView;
        ConstraintLayout constraintLayout;

        ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            dateTextView = view.findViewById(R.id.article_date);
            authorTextView = view.findViewById(R.id.article_author);
            constraintLayout = view.findViewById(R.id.list_item_constraint_layout);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (recyclerView != null) {
            StaggeredGridLayoutManager manager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            if (manager != null && recyclerView.getAdapter() != null) {
                int[] positions = new int[manager.getSpanCount()];
                manager.findFirstCompletelyVisibleItemPositions(positions);
                outState.putInt(RV_POSITION,positions[0]);
            }
        }
    }
}
