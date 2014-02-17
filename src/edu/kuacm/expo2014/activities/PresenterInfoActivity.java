package edu.kuacm.expo2014.activities;

import edu.kuacm.expo2014.R;
import edu.kuacm.expo2014.fragments.PresenterInfoListFragment;
import edu.kuacm.expo2014.model.Presenter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

public class PresenterInfoActivity extends ActionBarActivity {

	public static final String EXTRA_PRESENTER = "presenter";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		Presenter presenter = getIntent().getParcelableExtra(EXTRA_PRESENTER);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.presenter_info);

		if (savedInstanceState == null) {
			Fragment f = PresenterInfoListFragment.newInstance(presenter);
			getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}
}