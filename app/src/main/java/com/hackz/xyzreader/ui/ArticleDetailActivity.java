package com.hackz.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.hackz.xyzreader.R;
import com.hackz.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;

public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private Cursor cursor;
    private long startId;
    private Toolbar toolbar;
    private ImageView articleImage;
    private static String titleFab;
    private static String authorFab;
    private static final String EXTRA_ID = "article selected extra id";

    private ViewPager viewPager;
    private ArticleDetailActivity.MyPagerAdapter pagerAdapter;
    private FloatingActionButton fab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_article_detail);
        toolbar = findViewById(R.id.toolbar_detail);
        articleImage = findViewById(R.id.article_detail_photo);
        fab = findViewById(R.id.fab_share);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareArticle();
            }
        });
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        getSupportLoaderManager().initLoader(0, null, this);

        pagerAdapter = new ArticleDetailActivity.MyPagerAdapter(getSupportFragmentManager());
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {


            @Override
            public void onPageSelected(int position) {
                if (cursor != null) {
                    cursor.moveToPosition(position);
                }
            }
        });


        if (savedInstanceState == null) {
            if (getIntent() != null) {
                startId = getIntent().getLongExtra(EXTRA_ID, 0);
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.share){
            shareArticle();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareArticle(){
        if(cursor != null && authorFab != null && titleFab != null){
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_article));
            String message = String.format(getString(R.string.share_message), titleFab, authorFab);
            intent.putExtra(Intent.EXTRA_TEXT, message);
            startActivity(Intent.createChooser(intent, getString(R.string.share_title)));
        }
    }



    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }


    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        this.cursor = cursor;
        pagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (startId > 0) {
            while (cursor.moveToNext()) {
                if (this.cursor.getLong(ArticleLoader.Query._ID) == startId) {
                    final int position = this.cursor.getPosition();
                    viewPager.setCurrentItem(position, false);
                    break;
                }
            }
            startId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        cursor = null;
        pagerAdapter.notifyDataSetChanged();
    }


    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            cursor.moveToPosition(position);
            String photoUrl = cursor.getString(ArticleLoader.Query.PHOTO_URL);
            titleFab = cursor.getString(ArticleLoader.Query.TITLE);
            authorFab = cursor.getString(ArticleLoader.Query.AUTHOR);
            Picasso.get().load(photoUrl).placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder).into(articleImage);

        }


        @Override
        public Fragment getItem(int position) {
            cursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(cursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (cursor != null) ? cursor.getCount() : 0;
        }
    }
}
