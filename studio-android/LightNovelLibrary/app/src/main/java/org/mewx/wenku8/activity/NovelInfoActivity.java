package org.mewx.wenku8.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.umeng.analytics.MobclickAgent;

import org.apache.http.NameValuePair;
import org.mewx.wenku8.R;
import org.mewx.wenku8.global.GlobalConfig;
import org.mewx.wenku8.global.api.ChapterInfo;
import org.mewx.wenku8.global.api.NovelItemMeta;
import org.mewx.wenku8.global.api.OldNovelContentParser;
import org.mewx.wenku8.global.api.VolumeList;
import org.mewx.wenku8.global.api.Wenku8API;
import org.mewx.wenku8.global.api.Wenku8Error;
import org.mewx.wenku8.global.api.Wenku8Parser;
import org.mewx.wenku8.util.LightCache;
import org.mewx.wenku8.util.LightNetwork;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * Created by MewX on 2015/5/13.
 */
public class NovelInfoActivity extends AppCompatActivity {

    // constant
    private final String FromLocal = "fav";

    // private vars
    private int aid = 1;
    private String from = "";
    private boolean isLoading = true;
    private Toolbar mToolbar = null;
    private RelativeLayout rlMask = null; // mask layout
    private LinearLayout mLinearLayout = null;
    private LinearLayout llCardLayout = null;
    private ImageView ivNovelCover = null;
    private TextView tvNovelTitle = null;
    private TextView tvNovelAuthor = null;
    private TextView tvNovelStatus = null;
    private TextView tvNovelUpdate = null;
    private TableRow tvNovelShortIntro = null; // need hide
    private TextView tvNovelFullIntro = null;
    private ImageButton ibNovelOption = null; // need hide
    private MaterialDialog pDialog = null;
    private FloatingActionButton fabFavorate = null;
    private FloatingActionButton fabDownload = null;
    private FloatingActionsMenu famMenu = null;
    private SmoothProgressBar spb = null;
    private NovelItemMeta mNovelItemMeta = null;
    private List<VolumeList> listVolume = null;
    private String novelFullMeta = null, novelFullIntro = null, novelFullVolume = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_novel_info);

        // fetch values
        aid = getIntent().getIntExtra("aid", 1);
        from = getIntent().getStringExtra("from");
        String title = getIntent().getStringExtra("title");

        // set indicator enable
        mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(mToolbar);
        final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        if(getSupportActionBar() != null && upArrow != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            upArrow.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }

        // change status bar color tint, and this require SDK16
        if (Build.VERSION.SDK_INT >= 16 ) {
            // Android API 22 has more effects on status bar, so ignore

            // create our manager instance after the content view is set
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            // enable all tint
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setNavigationBarTintEnabled(true);
            tintManager.setTintAlpha(0.15f);
            // set all color
            tintManager.setTintColor(getResources().getColor(android.R.color.black));

        }

        // UIL setting
        if(ImageLoader.getInstance() == null || !ImageLoader.getInstance().isInited()) {
            GlobalConfig.initImageLoader(this);
        }

        // get views
        rlMask = (RelativeLayout) findViewById(R.id.white_mask);
        mLinearLayout = (LinearLayout) findViewById(R.id.novel_info_scroll);
        llCardLayout = (LinearLayout) findViewById(R.id.item_card);
        ivNovelCover = (ImageView) findViewById(R.id.novel_cover);
        tvNovelTitle = (TextView) findViewById(R.id.novel_title);
        tvNovelAuthor = (TextView) findViewById(R.id.novel_author);
        tvNovelStatus = (TextView) findViewById(R.id.novel_status);
        tvNovelUpdate = (TextView) findViewById(R.id.novel_update);
        tvNovelShortIntro = (TableRow) findViewById(R.id.novel_intro_row);
        tvNovelFullIntro = (TextView) findViewById(R.id.novel_intro_full);
        ibNovelOption = (ImageButton) findViewById(R.id.novel_option);
        fabFavorate = (FloatingActionButton) findViewById(R.id.fab_favorate);
        fabDownload = (FloatingActionButton) findViewById(R.id.fab_download);
        famMenu = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        spb = (SmoothProgressBar) findViewById(R.id.spb);

        // hide view and set colors
        tvNovelTitle.setText(title);
        ImageLoader.getInstance().displayImage(Wenku8API.getCoverURL(aid), ivNovelCover); // move to onCreateView!
        tvNovelShortIntro.setVisibility(TextView.GONE);
        ibNovelOption.setVisibility(ImageButton.INVISIBLE);
        fabFavorate.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP);
        fabDownload.setColorFilter(getResources().getColor(R.color.default_white), PorterDuff.Mode.SRC_ATOP);
        llCardLayout.setBackgroundResource(R.color.menu_transparent);
        if (GlobalConfig.testInLocalBookshelf(aid)) {
            fabFavorate.setIcon(R.drawable.ic_favorate_pressed);
        }

        // fetch all info
        getSupportActionBar().setTitle(R.string.action_novel_info);
        spb.setVisibility(View.INVISIBLE); // wait for runnable
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                spb.setVisibility(View.VISIBLE);
                if(from.equals(FromLocal))
                    refreshInfoFromLocal();
                else
                    refreshInfoFromCloud();
            }
        }, 500);


        // set on click listeners
        famMenu.setOnFloatingActionsMenuUpdateListener(new FloatingActionsMenu.OnFloatingActionsMenuUpdateListener() {
            @Override
            public void onMenuExpanded() {
                rlMask.setVisibility(View.VISIBLE);
            }

            @Override
            public void onMenuCollapsed() {
                rlMask.setVisibility(View.INVISIBLE);
            }
        });
        rlMask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Collapse the fam
                if (famMenu.isExpanded())
                    famMenu.collapse();
            }
        });
        tvNovelTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isLoading) {
                    Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.system_loading_please_wait), Toast.LENGTH_SHORT).show();
                    return;
                }

                // show aid: title
                // Snackbar.make(mLinearLayout, aid + ": " + mNovelItemMeta.title, Snackbar.LENGTH_SHORT).show();
                new MaterialDialog.Builder(NovelInfoActivity.this)
                        .theme(Theme.LIGHT)
                        .titleColor(R.color.dlgTitleColor)
                        .backgroundColorRes(R.color.dlgBackgroundColor)
                        .contentColorRes(R.color.dlgContentColor)
                        .positiveColorRes(R.color.dlgPositiveButtonColor)
                        .title(R.string.dialog_content_novel_title)
                        .content(aid + ": " + mNovelItemMeta.title)
                        .contentGravity(GravityEnum.CENTER)
                        .positiveText(R.string.dialog_positive_known)
                        .show();
            }
        });
        fabFavorate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isLoading) {
                    Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.system_loading_please_wait), Toast.LENGTH_SHORT).show();
                    return;
                }

                // add to favorate
                if(GlobalConfig.testInLocalBookshelf(aid)) {
                    new MaterialDialog.Builder(NovelInfoActivity.this).callback(new MaterialDialog.ButtonCallback() {
                                @Override
                                public void onPositive(MaterialDialog dialog) {
                                    super.onPositive(dialog);

                                    // already in bookshelf
                                    for (VolumeList tempVl : listVolume) {
                                        for (ChapterInfo tempCi : tempVl.chapterList) {
                                            LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "novel" + File.separator + tempCi.cid + ".xml");
                                            LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "novel" + File.separator + tempCi.cid + ".xml");
                                        }
                                    }

                                    // delete files
                                    LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "intro" + File.separator + aid + "-intro.xml");
                                    LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "intro" + File.separator + aid + "-introfull.xml");
                                    LightCache.deleteFile(GlobalConfig.getFirstFullSaveFilePath(), "intro" + File.separator + aid + "-volume.xml");
                                    LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "intro" + File.separator + aid + "-intro.xml");
                                    LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "intro" + File.separator + aid + "-introfull.xml");
                                    LightCache.deleteFile(GlobalConfig.getSecondFullSaveFilePath(), "intro" + File.separator + aid + "-volume.xml");

                                    // remove from bookshelf
                                    GlobalConfig.removeFromLocalBookshelf(aid);
                                    if (!GlobalConfig.testInLocalBookshelf(aid)) { // not in
                                        Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.bookshelf_removed), Toast.LENGTH_SHORT).show();
                                        fabFavorate.setIcon(R.drawable.ic_favorate);
                                    } else {
                                        Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.bookshelf_error), Toast.LENGTH_SHORT).show();
                                    }

                                }
                            })
                            .theme(Theme.LIGHT)
                            .backgroundColorRes(R.color.dlgBackgroundColor)
                            .contentColorRes(R.color.dlgContentColor)
                            .positiveColorRes(R.color.dlgPositiveButtonColor)
                            .negativeColorRes(R.color.dlgNegativeButtonColor)
                            .content(R.string.dialog_content_sure_to_unfav)
                            .contentGravity(GravityEnum.CENTER)
                            .positiveText(R.string.dialog_positive_yes)
                            .negativeText(R.string.dialog_negative_preferno)
                            .show();
                }
                else {
                    // not in bookshelf, add it to.
                    GlobalConfig.writeFullFileIntoSaveFolder("intro", aid + "-intro.xml", novelFullMeta);
                    GlobalConfig.writeFullFileIntoSaveFolder("intro", aid + "-introfull.xml", novelFullIntro);
                    GlobalConfig.writeFullFileIntoSaveFolder("intro", aid+ "-volume.xml", novelFullVolume);
                    GlobalConfig.addToLocalBookshelf(aid);
                    if (GlobalConfig.testInLocalBookshelf(aid)) { // in
                        Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.bookshelf_added), Toast.LENGTH_SHORT).show();
                        fabFavorate.setIcon(R.drawable.ic_favorate_pressed);
                    } else {
                        Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.bookshelf_error), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        fabDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isLoading) {
                    Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.system_loading_please_wait), Toast.LENGTH_SHORT).show();
                    return;
                }
                else if(!GlobalConfig.testInLocalBookshelf(aid)) {
                    Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.system_fav_it_first), Toast.LENGTH_SHORT).show();
                    return;
                }

                // download / update activity or verify downloading action (add to queue)
                // use list dialog to provide more functions
                new MaterialDialog.Builder(NovelInfoActivity.this)
                        .theme(Theme.LIGHT)
                        .title(R.string.dialog_title_choose_download_option)
                        .backgroundColorRes(R.color.dlgBackgroundColor)
                        .titleColor(R.color.dlgTitleColor)
                        .negativeText(R.string.dialog_negative_pass)
                        .negativeColorRes(R.color.dlgNegativeButtonColor)
                        .itemsGravity(GravityEnum.CENTER)
                        .items(R.array.download_option)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                                /**
                                 * 0 <string name="dialog_option_check_for_update">检查更新</string>
                                 * 1 <string name="dialog_option_update_uncached_volumes">更新下载</string>
                                 * 2 <string name="dialog_option_force_update_all">覆盖下载</string>
                                 * 3 <string name="dialog_option_select_and_update">分卷下载</string>
                                 */
                                switch (which) {
                                    case 0:
                                        new MaterialDialog.Builder(NovelInfoActivity.this)
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onPositive(MaterialDialog dialog) {
                                                        super.onPositive(dialog);

                                                        // async task
                                                        isLoading = true;
                                                        final AsyncUpdateCacheTask auct = new AsyncUpdateCacheTask();
                                                        auct.execute(aid, 0);

                                                        // show progress
                                                        pDialog = new MaterialDialog.Builder(NovelInfoActivity.this)
                                                                .theme(Theme.LIGHT)
                                                                .content(R.string.dialog_content_downloading)
                                                                .progress(false, 1, true)
                                                                .cancelable(true)
                                                                .cancelListener(new DialogInterface.OnCancelListener() {
                                                                    @Override
                                                                    public void onCancel(DialogInterface dialog) {
                                                                        isLoading = false;
                                                                        auct.cancel(true);
                                                                        pDialog.dismiss();
                                                                        pDialog = null;
                                                                    }
                                                                })
                                                                .show();

                                                        pDialog.setProgress(0);
                                                        pDialog.setMaxProgress(1);
                                                        pDialog.show();
                                                    }
                                                })
                                                .theme(Theme.LIGHT)
                                                .backgroundColorRes(R.color.dlgBackgroundColor)
                                                .contentColorRes(R.color.dlgContentColor)
                                                .positiveColorRes(R.color.dlgPositiveButtonColor)
                                                .negativeColorRes(R.color.dlgNegativeButtonColor)
                                                .content(R.string.dialog_content_verify_update)
                                                .contentGravity(GravityEnum.CENTER)
                                                .positiveText(R.string.dialog_positive_likethis)
                                                .negativeText(R.string.dialog_negative_preferno)
                                                .show();
                                        break;

                                    case 1:
                                        new MaterialDialog.Builder(NovelInfoActivity.this)
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onPositive(MaterialDialog dialog) {
                                                        super.onPositive(dialog);

                                                        // async task
                                                        isLoading = true;
                                                        final AsyncUpdateCacheTask auct = new AsyncUpdateCacheTask();
                                                        auct.execute(aid, 1);

                                                        // show progress
                                                        pDialog = new MaterialDialog.Builder(NovelInfoActivity.this)
                                                                .theme(Theme.LIGHT)
                                                                .content(R.string.dialog_content_downloading)
                                                                .progress(false, 1, true)
                                                                .cancelable(true)
                                                                .cancelListener(new DialogInterface.OnCancelListener() {
                                                                    @Override
                                                                    public void onCancel(DialogInterface dialog) {
                                                                        isLoading = false;
                                                                        auct.cancel(true);
                                                                        pDialog.dismiss();
                                                                        pDialog = null;
                                                                    }
                                                                })
                                                                .show();

                                                        pDialog.setProgress(0);
                                                        pDialog.setMaxProgress(1);
                                                        pDialog.show();
                                                    }
                                                })
                                                .theme(Theme.LIGHT)
                                                .backgroundColorRes(R.color.dlgBackgroundColor)
                                                .contentColorRes(R.color.dlgContentColor)
                                                .positiveColorRes(R.color.dlgPositiveButtonColor)
                                                .negativeColorRes(R.color.dlgNegativeButtonColor)
                                                .content(R.string.dialog_content_verify_download)
                                                .contentGravity(GravityEnum.CENTER)
                                                .positiveText(R.string.dialog_positive_likethis)
                                                .negativeText(R.string.dialog_negative_preferno)
                                                .show();
                                        break;

                                    case 2:
                                        new MaterialDialog.Builder(NovelInfoActivity.this)
                                                .callback(new MaterialDialog.ButtonCallback() {
                                                    @Override
                                                    public void onPositive(MaterialDialog dialog) {
                                                        super.onPositive(dialog);

                                                        // async task
                                                        isLoading = true;
                                                        final AsyncUpdateCacheTask auct = new AsyncUpdateCacheTask();
                                                        auct.execute(aid, 2);

                                                        // show progress
                                                        pDialog = new MaterialDialog.Builder(NovelInfoActivity.this)
                                                                .theme(Theme.LIGHT)
                                                                .content(R.string.dialog_content_downloading)
                                                                .progress(false, 1, true)
                                                                .cancelable(true)
                                                                .cancelListener(new DialogInterface.OnCancelListener() {
                                                                    @Override
                                                                    public void onCancel(DialogInterface dialog) {
                                                                        isLoading = false;
                                                                        auct.cancel(true);
                                                                        pDialog.dismiss();
                                                                        pDialog = null;
                                                                    }
                                                                })
                                                                .show();

                                                        pDialog.setProgress(0);
                                                        pDialog.setMaxProgress(1);
                                                        pDialog.show();
                                                    }
                                                })
                                                .theme(Theme.LIGHT)
                                                .backgroundColorRes(R.color.dlgBackgroundColor)
                                                .contentColorRes(R.color.dlgContentColor)
                                                .positiveColorRes(R.color.dlgPositiveButtonColor)
                                                .negativeColorRes(R.color.dlgNegativeButtonColor)
                                                .content(R.string.dialog_content_verify_force_update)
                                                .contentGravity(GravityEnum.CENTER)
                                                .positiveText(R.string.dialog_positive_likethis)
                                                .negativeText(R.string.dialog_negative_preferno)
                                                .show();
                                        break;

                                    case 3:
                                        // TODO: new activity to hold selectable items
                                        Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.system_wait_for_next_version), Toast.LENGTH_SHORT).show();
                                        break;
                                }

                            }
                        })
                        .show();
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(getSupportActionBar()!=null)
            getSupportActionBar().setTitle(getResources().getString(R.string.action_novel_info));
        getMenuInflater().inflate(R.menu.menu_novel_info, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            if(Build.VERSION.SDK_INT < 21)
                finish();
            else
                finishAfterTransition(); // end directly
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onBackPressed() {
        // end famMenu first
        if(famMenu.isExpanded()) {
            famMenu.collapse();
            return;
        }

        // normal exit
        super.onBackPressed();
    }

    private class FetchInfoAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        boolean fromLocal = false;

        @Override
        protected Integer doInBackground(Integer... params) {
            // transfer '1' to this task represent loading from local
            if(params != null && params.length == 1 && params[0] == 1)
                fromLocal = true;

            // get novel full meta
            try {
                if(fromLocal) {
                    novelFullMeta = GlobalConfig.loadFullFileFromSaveFolder("intro", aid + "-intro.xml");
                    if(novelFullMeta == null || novelFullMeta.equals("")) return -9;
                }
                else {
                    List<NameValuePair> nvpMetaRequest = new ArrayList<NameValuePair>();
                    nvpMetaRequest.add(Wenku8API.getNovelFullMeta(aid, GlobalConfig.getCurrentLang()));
                    byte[] byteNovelFullMeta = LightNetwork.LightHttpPost(Wenku8API.getBaseURL(), nvpMetaRequest);
                    if (byteNovelFullMeta == null) return -1;
                    novelFullMeta = new String(byteNovelFullMeta, "UTF-8"); // save
                }
                mNovelItemMeta = Wenku8Parser.parsetNovelFullMeta(novelFullMeta);
                if(mNovelItemMeta == null) return -1;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return -2;
            }
            publishProgress(1); // procedure 1/3

            // get novel full intro
            try {
                if(fromLocal) {
                    novelFullIntro = GlobalConfig.loadFullFileFromSaveFolder("intro", aid + "-introfull.xml");
                    if(novelFullIntro == null || novelFullIntro.equals("")) return -9;
                }
                else {
                    List<NameValuePair> nvpFullIntroRequest = new ArrayList<NameValuePair>();
                    nvpFullIntroRequest.add(Wenku8API.getNovelFullIntro(aid, GlobalConfig.getCurrentLang()));
                    byte[] byteNovelFullInfo = LightNetwork.LightHttpPost(Wenku8API.getBaseURL(), nvpFullIntroRequest);
                    if (byteNovelFullInfo == null) return -1;
                    novelFullIntro = new String(byteNovelFullInfo, "UTF-8"); // save
                }
                mNovelItemMeta.fullIntro = novelFullIntro;
                if(mNovelItemMeta.fullIntro == null) return -1;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return -2;
            }
            publishProgress(2);

            // get novel chapter list
            try {
                if(fromLocal) {
                    novelFullVolume = GlobalConfig.loadFullFileFromSaveFolder("intro", aid + "-volume.xml");
                    if(novelFullVolume == null || novelFullVolume.equals("")) return -9;
                }
                else {
                    List<NameValuePair> nvpChapterListRequest = new ArrayList<NameValuePair>();
                    nvpChapterListRequest.add(Wenku8API.getNovelIndex(aid, GlobalConfig.getCurrentLang()));
                    byte[] byteNovelChapterList = LightNetwork.LightHttpPost(Wenku8API.getBaseURL(), nvpChapterListRequest);
                    if (byteNovelChapterList == null) return -1;
                    novelFullVolume = new String(byteNovelChapterList, "UTF-8"); // save
                }

                listVolume = Wenku8Parser.getVolumeList(novelFullVolume);
                if(listVolume == null) return -1;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return -2;
            }
            publishProgress(3); // procedure 3/3

            // Check local volume files exists, express in another color
            for(VolumeList vl : listVolume) {
                for(ChapterInfo ci : vl.chapterList) {
                    if(!LightCache.testFileExist(GlobalConfig.getFirstFullSaveFilePath() + "novel" + File.separator + ci.cid + ".xml")
                            && !LightCache.testFileExist(GlobalConfig.getSecondFullSaveFilePath() + File.separator + "novel" + ci.cid + ".xml"))
                        break;
                    //String content = GlobalConfig.loadFullFileFromSaveFolder("novel", listVolume.get(i).chapterList.get(j).cid + ".xml");
                    //List<OldNovelContentParser.NovelContent> listImage = OldNovelContentParser.NovelContentParser_onlyImage(content);

                    if(vl.chapterList.indexOf(ci) == vl.chapterList.size() - 1)
                        vl.inLocal = true;
                }
            }

            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            switch (values[0]) {
                case 1:
                    // update general info
                    tvNovelTitle.setText(mNovelItemMeta.title);
                    tvNovelAuthor.setText(mNovelItemMeta.author);
                    tvNovelStatus.setText(mNovelItemMeta.bookStatus);
                    tvNovelUpdate.setText(mNovelItemMeta.lastUpdate);
                    NovelInfoActivity.this.getSupportActionBar().setTitle(mNovelItemMeta.title); // set action bar title

                    break;

                case 2:
                    //update novel info full
                    tvNovelFullIntro.setText(mNovelItemMeta.fullIntro);

                    break;

                case 3:
                    // let onPostExecute do
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            if( integer == -1 ) {
                Toast.makeText(NovelInfoActivity.this, "FetchInfoAsyncTask:onPostExecute network error", Toast.LENGTH_SHORT).show();
                return;
            }
            else if(integer == -9) {
                Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.bookshelf_intro_load_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            else if(integer < 0)
                return; // ignore other exceptions

            // remove all TextView(in CardView, in RelativeView)
            if(mLinearLayout.getChildCount() >= 3)
                mLinearLayout.removeViews(2, mLinearLayout.getChildCount() - 2);

            for(final VolumeList vl : listVolume) {
                // get view
                RelativeLayout rl = (RelativeLayout) LayoutInflater.from(NovelInfoActivity.this).inflate(R.layout.view_novel_chapter_item, null);

                // set text and listeners
                TextView tv = (TextView) rl.findViewById(R.id.chapter_title);
                tv.setText(vl.volumeName);
                if(vl.inLocal)
                    ((TextView) rl.findViewById(R.id.chapter_status)).setText(getResources().getString(R.string.bookshelf_inlocal));
                rl.findViewById(R.id.chapter_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // jump to chapter select activity
                        Intent intent = new Intent(NovelInfoActivity.this, NovelChapterActivity.class);
                        intent.putExtra("aid", aid);
                        intent.putExtra("volume", vl);
                        intent.putExtra("from", from);
                        startActivity(intent);
                    }
                });

                // add to scroll view
                mLinearLayout.addView(rl);
            }

            isLoading = false;
            spb.progressiveStop();
            super.onPostExecute(integer);
        }
    }

    class AsyncUpdateCacheTask extends AsyncTask<Integer, Integer, Wenku8Error.ErrorCode> {
        // in: Aid, OperationType
        // out: current loading
        String volumeXml, introXml;
        List<VolumeList> vl = null;
        List<String> imageList = null; // add one and save once
        private NovelItemMeta ni;
        int size_a = 0, current = 0;

        @Override
        protected Wenku8Error.ErrorCode doInBackground(Integer... params) {
            if(params == null || params.length < 2) return Wenku8Error.ErrorCode.PARAM_COUNT_NOT_MATCHED;
            int taskaid = params[0];
            int operationType = params[1]; // type = 0, 1, 2

            // get full range online, always
            try {
                // fetch intro
                if (!isLoading)
                    return Wenku8Error.ErrorCode.USER_CANCELLED_TASK; // cancel
                List<NameValuePair> targVarListVolume = new ArrayList<NameValuePair>();
                targVarListVolume.add(Wenku8API.getNovelIndex(taskaid, GlobalConfig.getCurrentLang()));
                byte[] tempVolumeXml = LightNetwork.LightHttpPost(Wenku8API.getBaseURL(), targVarListVolume);
                if (tempVolumeXml == null) return Wenku8Error.ErrorCode.NETWORK_ERROR; // network error
                volumeXml = new String(tempVolumeXml, "UTF-8");

                if (!isLoading)
                    return Wenku8Error.ErrorCode.USER_CANCELLED_TASK; // cancel
                List<NameValuePair> targVarList = new ArrayList<NameValuePair>();
                targVarList.add(Wenku8API.getNovelFullMeta(taskaid, GlobalConfig.getCurrentLang()));
                byte[] tempIntroXml = LightNetwork.LightHttpPost(Wenku8API.getBaseURL(), targVarList);
                if (tempIntroXml == null) return Wenku8Error.ErrorCode.NETWORK_ERROR; // network error
                introXml = new String(tempIntroXml, "UTF-8");

                // parse into structures
                vl = Wenku8Parser.getVolumeList(volumeXml);
                ni = Wenku8Parser.parsetNovelFullMeta(introXml);
                if (vl == null || ni == null) return Wenku8Error.ErrorCode.XML_PARSE_FAILED; // parse failed

                if (!isLoading)
                    return Wenku8Error.ErrorCode.USER_CANCELLED_TASK; // calcel
                List<NameValuePair> targIntro = new ArrayList<NameValuePair>();
                targIntro.add(Wenku8API.getNovelFullIntro(ni.aid, GlobalConfig.getCurrentLang()));
                byte[] tempFullIntro = LightNetwork.LightHttpPost(Wenku8API.getBaseURL(), targIntro);
                if (tempFullIntro == null) return Wenku8Error.ErrorCode.NETWORK_ERROR; // network error
                ni.fullIntro = new String(tempFullIntro, "UTF-8");

                // write into saved file
                GlobalConfig.writeFullFileIntoSaveFolder("intro", taskaid + "-intro.xml", introXml);
                GlobalConfig.writeFullFileIntoSaveFolder("intro", taskaid + "-introfull.xml", ni.fullIntro);
                GlobalConfig.writeFullFileIntoSaveFolder("intro", taskaid + "-volume.xml", volumeXml);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if(operationType == 0) return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED; // update info

            // calc size
            for (VolumeList tempVl : vl) {
                size_a += tempVl.chapterList.size();
            }
            pDialog.setMaxProgress(size_a);

            // cache each cid to save the whole book
            // and will need to download all the images
            for (VolumeList tempVl : vl) {
                for (ChapterInfo tempCi : tempVl.chapterList) {
                    try {
                        List<NameValuePair> targVar = new ArrayList<NameValuePair>();
                        targVar.add(Wenku8API.getNovelContent(ni.aid, tempCi.cid, GlobalConfig.getCurrentLang()));

                        // load from local first
                        if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK; // calcel
                        String xml = GlobalConfig.loadFullFileFromSaveFolder("novel", tempCi.cid + ".xml"); // prevent empty file
                        if (xml == null || xml.length() == 0 || operationType == 2) {
                            byte[] tempXml = LightNetwork.LightHttpPost(Wenku8API.getBaseURL(), targVar);
                            if (tempXml == null) return Wenku8Error.ErrorCode.NETWORK_ERROR; // network error
                            xml = new String(tempXml, "UTF-8");

                            // save file (cid.xml), didn't format it future version may format it for better performance
                            GlobalConfig.writeFullFileIntoSaveFolder("novel", tempCi.cid + ".xml", xml);
                        }

                        // cache image
                        if (GlobalConfig.doCacheImage()) {
                            List<OldNovelContentParser.NovelContent> nc = OldNovelContentParser.NovelContentParser_onlyImage(xml);
                            if (nc == null) return Wenku8Error.ErrorCode.NETWORK_ERROR;

                            for (int i = 0; i < nc.size(); i++) {
                                if (nc.get(i).type == 'i') {
                                    pDialog.setMaxProgress(++size_a);

                                    // save this images, judge exist first
                                    String imgFileName = GlobalConfig
                                            .generateImageFileNameByURL(nc
                                                    .get(i).content);
                                    if (!LightCache.testFileExist(GlobalConfig.getFirstFullSaveFilePath()
                                            + GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)
                                            && !LightCache.testFileExist(GlobalConfig.getSecondFullSaveFilePath()
                                            + GlobalConfig.imgsSaveFolderName + File.separator + imgFileName)
                                            || operationType == 2) {
                                        // neither of the file exist
                                        byte[] fileContent = LightNetwork.LightHttpDownload(nc.get(i).content);
                                        if (fileContent == null) return Wenku8Error.ErrorCode.NETWORK_ERROR; // network error
                                        if (!LightCache.saveFile(GlobalConfig.getFirstFullSaveFilePath()
                                                        + GlobalConfig.imgsSaveFolderName + File.separator,
                                                imgFileName, fileContent, true)) // fail
                                            // to first path
                                            LightCache.saveFile(GlobalConfig.getSecondFullSaveFilePath()
                                                            + GlobalConfig.imgsSaveFolderName + File.separator,
                                                    imgFileName, fileContent, true);
                                    }

                                    if (!isLoading) return Wenku8Error.ErrorCode.USER_CANCELLED_TASK;
                                    publishProgress(++current); // update
                                    // progress
                                }
                            }
                        }
                        publishProgress(++current); // update progress

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }

            return Wenku8Error.ErrorCode.SYSTEM_1_SUCCEEDED;
        }

        @Override
        protected void onProgressUpdate(Integer... values)
        {
            if (pDialog != null)
                pDialog.setProgress(values[0]);
        }

        protected void onPostExecute(Wenku8Error.ErrorCode result)
        {
            if (result == Wenku8Error.ErrorCode.USER_CANCELLED_TASK) {
                // user cancelled
                Toast.makeText(NovelInfoActivity.this, "User cancelled!", Toast.LENGTH_LONG).show();
                if (pDialog != null)
                    pDialog.dismiss();
                onResume();
                isLoading = false;
                return;
            } else if (result == Wenku8Error.ErrorCode.NETWORK_ERROR) {
                Toast.makeText(NovelInfoActivity.this, getResources().getString(R.string.system_network_error), Toast.LENGTH_LONG).show();
                if (pDialog != null)
                    pDialog.dismiss();
                onResume();
                isLoading = false;
                return;
            } else if (result == Wenku8Error.ErrorCode.XML_PARSE_FAILED) {
                Toast.makeText(NovelInfoActivity.this, "Parse failed!", Toast.LENGTH_LONG).show();
                if (pDialog != null)
                    pDialog.dismiss();
                onResume();
                isLoading = false;
                return;
            }

            // cache successfully
            Toast.makeText(NovelInfoActivity.this, "OK", Toast.LENGTH_LONG).show();
            isLoading = false;
            if (pDialog != null)
                pDialog.dismiss();

            refreshInfoFromLocal();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    private void refreshInfoFromLocal() {
        isLoading = true;
        spb.progressiveStart();
        FetchInfoAsyncTask fetchInfoAsyncTask = new FetchInfoAsyncTask();
        fetchInfoAsyncTask.execute(1); // load from local
    }

    private void refreshInfoFromCloud() {
        isLoading = true;
        spb.progressiveStart();
        FetchInfoAsyncTask fetchInfoAsyncTask = new FetchInfoAsyncTask();
        fetchInfoAsyncTask.execute();
    }
}
