package com.zfdang.zsmth_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
//import android.support.v7.widget.StaggeredGridLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.jude.swipbackhelper.SwipeBackHelper;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.footer.ClassicsFooter;
import com.scwang.smartrefresh.layout.header.ClassicsHeader;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.zfdang.SMTHApplication;
//import com.zfdang.zsmth_android.helpers.ActivityUtils;
import com.zfdang.zsmth_android.fresco.WrapContentDraweeView;
import com.zfdang.zsmth_android.helpers.RecyclerViewUtil;
import com.zfdang.zsmth_android.models.Attachment;
import com.zfdang.zsmth_android.models.Board;
import com.zfdang.zsmth_android.models.ComposePostContext;
import com.zfdang.zsmth_android.models.Post;
import com.zfdang.zsmth_android.models.PostActionAlertDialogItem;
import com.zfdang.zsmth_android.models.PostListContent;
import com.zfdang.zsmth_android.models.Topic;
import com.zfdang.zsmth_android.newsmth.AjaxResponse;
import com.zfdang.zsmth_android.newsmth.SMTHHelper;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.onekeyshare.OnekeyShare;
//import github.nisrulz.screenshott.ScreenShott;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
import java.io.File;
//import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;
//import java.util.Stack;

import okhttp3.ResponseBody;

/**
 * An activity representing a single Topic detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link BoardTopicActivity}.
 */
public class PostListActivity extends SMTHBaseActivity
    implements View.OnClickListener, OnTouchListener, RecyclerViewGestureListener.OnItemLongClickListener, PopupLikeWindow.OnLikeInterface,
    PopupForwardWindow.OnForwardInterface {

  private static final String TAG = "PostListActivity";
  public RecyclerView mRecyclerView = null;
  private TextView mTitle = null;
  private EditText mPageNo = null;

  public int mCurrentPageNo = 1;
  public static int mCurrentReadPageNo = 1;
  public static int mTotalPageNo =0;
  private String mFilterUser = null;

  private static Topic mTopic = null;
  private static int lastOffset =0;
  private static int lastPosition =0;

  static private final int POST_PER_PAGE = 10;

  static {
    ClassicsHeader.REFRESH_HEADER_PULLDOWN = "??????????????????";
    ClassicsHeader.REFRESH_HEADER_REFRESHING = "????????????...";
    ClassicsHeader.REFRESH_HEADER_LOADING = "????????????...";
    ClassicsHeader.REFRESH_HEADER_RELEASE = "??????????????????";
    ClassicsHeader.REFRESH_HEADER_FINISH = "????????????";
    ClassicsHeader.REFRESH_HEADER_FAILED = "????????????";
    ClassicsHeader.REFRESH_HEADER_LASTTIME = "???????????? M-d HH:mm";

    ClassicsFooter.REFRESH_FOOTER_PULLUP = "??????????????????";
    ClassicsFooter.REFRESH_FOOTER_RELEASE = "??????????????????";
    ClassicsFooter.REFRESH_FOOTER_REFRESHING = "????????????...";
    ClassicsFooter.REFRESH_FOOTER_LOADING = "?????????????????????...";
    ClassicsFooter.REFRESH_FOOTER_FINISH = "????????????";
    ClassicsFooter.REFRESH_FOOTER_FAILED = "????????????";
    ClassicsFooter.REFRESH_FOOTER_ALLLOADED = "??????????????????";
  }

  private SmartRefreshLayout mRefreshLayout;
  private String mFrom;

  private GestureDetector mGestureDetector;
  private LinearLayoutManager linearLayoutManager;

  @Override protected void onDestroy() {
    super.onDestroy();
    SwipeBackHelper.onDestroy(this);
  }

  @Override protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    SwipeBackHelper.onPostCreate(this);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if(requestCode == ComposePostActivity.COMPOSE_ACTIVITY_REQUEST_CODE) {
      // returned from Compose activity, refresh current post
      // TODO: check resultCode
      reloadPostList();
    }
    else if (requestCode == MainActivity.LOGIN_ACTIVITY_REQUEST_CODE) {
      if (resultCode == RESULT_OK) {
        SMTHApplication.activeUser.setId(Settings.getInstance().getUsername());
        Settings.getInstance().setUserOnline(true);
        UpdateNavigationViewHeaderNew();
      }
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  private void UpdateNavigationViewHeaderNew() {
    getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
    LayoutInflater factory = LayoutInflater.from(PostListActivity.this);

    //View layout = factory.inflate(R.layout.activity_main, null);
    View layout = factory.inflate(R.layout.nav_header_main, null);

    TextView mUsername = (TextView) layout.findViewById(R.id.nav_user_name);
    WrapContentDraweeView mAvatar = (WrapContentDraweeView) layout.findViewById(R.id.nav_user_avatar);

    if (SMTHApplication.isValidUser()) {
      // update user to login user
      mUsername.setText(SMTHApplication.activeUser.getId());
      String faceURL = SMTHApplication.activeUser.getFace_url();
      if (faceURL != null) {
        mAvatar.setImageFromStringURL(faceURL);
      }
    } else {
      // only user to guest
      mUsername.setText(getString(R.string.nav_header_click_to_login));
      mAvatar.setImageResource(R.drawable.ic_person_black_48dp);
    }

  }

  /**
   * Record current View
   */

  private void getPositionAndOffset() {
    LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    //Get first visible view
    View topView = layoutManager.getChildAt(0);
    if(topView != null) {
      lastOffset = topView.getTop();
      lastPosition = layoutManager.getPosition(topView);
    }
  }

  private void scrollToPosition() {
    if(mRecyclerView.getLayoutManager() != null && lastPosition >= 0) {
      ((LinearLayoutManager) mRecyclerView.getLayoutManager()).scrollToPositionWithOffset(lastPosition, lastOffset);
    }
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SwipeBackHelper.onCreate(this);

    setContentView(R.layout.activity_post_list);

    //LayoutInflater inflate = LayoutInflater.from(this);
    //View view = inflate.inflate(R.layout.activity_post_list,null);
    //setContentView(view);

    Toolbar toolbar = (Toolbar) findViewById(R.id.post_list_toolbar);
    setSupportActionBar(toolbar);

    // Show the Up button in the action bar.
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    mTitle = (TextView) findViewById(R.id.post_list_title);
    assert mTitle != null;
    mPageNo = (EditText) findViewById(R.id.post_list_page_no);
    assert mPageNo != null;

    // define swipe refresh function
    mRefreshLayout = (SmartRefreshLayout) findViewById(R.id.post_list_swipe_refresh_layout);
    mRefreshLayout.setEnableAutoLoadMore(false);
    mRefreshLayout.setEnableScrollContentWhenLoaded(false);
    mRefreshLayout.setEnableOverScrollBounce(false);

    /*
    if(!Settings.getInstance().isautoloadmore()) {
      mRefreshLayout.setEnableRefresh(true);
      mRefreshLayout.setEnableLoadMore(true);
    } else {
      mRefreshLayout.setEnableRefresh(true);
      mRefreshLayout.setEnableLoadMore(true);
    }
     */

    mRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
      @Override public void onRefresh(RefreshLayout refreshLayout) {
        // reload current page
        if(Settings.getInstance().isautoloadmore()) {
          reloadPostListWithoutAlert();
        }
        else { //Waterfall mode  insert item
          InsertPostListWithoutAlert();
        }
      }
    });
    mRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
      @Override public void onLoadMore(RefreshLayout refreshLayout) {
        // load next page if available
        if(Settings.getInstance().isautoloadmore()) {
          goToNextPage();
        }
        else {
          //goToNextPage();
          reloadPostListWithoutAlertNew();
        }
      }
    });

    mRecyclerView = (RecyclerView) findViewById(R.id.post_list);
    assert mRecyclerView != null;
    mRecyclerView.addItemDecoration(
        new DividerItemDecoration(this, LinearLayoutManager.VERTICAL, R.drawable.recyclerview_divider));
    linearLayoutManager = new WrapContentLinearLayoutManager(this);
    mRecyclerView.setLayoutManager(linearLayoutManager);
    mRecyclerView.setAdapter(new PostRecyclerViewAdapter(PostListContent.POSTS, this));

    //  holder.mView.setOnTouchListener(this); so the event will be sent from holder.mView
    mGestureDetector = new GestureDetector(SMTHApplication.getAppContext(), new RecyclerViewGestureListener(this, mRecyclerView));

    // get Board information from launcher
    Intent intent = getIntent();
    Topic topic = intent.getParcelableExtra(SMTHApplication.TOPIC_OBJECT);
    assert topic != null;
    mFrom = intent.getStringExtra(SMTHApplication.FROM_BOARD);
    // now onCreateOptionsMenu(...) is called again
    //        invalidateOptionsMenu();
    //        Log.d(TAG, String.format("Load post list for topic = %s, source = %s", topic.toString(), mFrom));

    // set onClick Lisetner for page navigator buttons

      findViewById(R.id.post_list_first_page).setOnClickListener(this);
      findViewById(R.id.post_list_pre_page).setOnClickListener(this);
      findViewById(R.id.post_list_next_page).setOnClickListener(this);
      findViewById(R.id.post_list_last_page).setOnClickListener(this);
      findViewById(R.id.post_list_go_page).setOnClickListener(this);


    LinearLayout navLayout = findViewById(R.id.post_list_action_layout);
    if (Settings.getInstance().hasPostNavBar()) {
      navLayout.setVisibility(View.VISIBLE);
    } else {
      navLayout.setVisibility(View.GONE);
    }
    initPostNavigationButtons();

    mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

      boolean isSlidingToLast = false;
      int mIndex = 0;

      @Override
      public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();

        super.onScrollStateChanged(recyclerView, newState);
        if (recyclerView.getLayoutManager() != null) {
          getPositionAndOffset();
        }
        if(!Settings.getInstance().isautoloadmore()) {
          if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            //LastItemPosition
            int lastVisiblePos = manager.findLastVisibleItemPosition();
            int totalItemCount = manager.getItemCount();

            //int firstVisibleItem = manager.findFirstVisibleItemPosition();
            // reach bottom
            if (lastVisiblePos == (totalItemCount - 1) && isSlidingToLast && (mCurrentPageNo < mTopic.getTotalPageNo())) {
              //Toast.makeText(getApplicationContext(), "????????????", Toast.LENGTH_SHORT).show();
              //goToNextPage();
              LoadMoreItems();
            }
            else if(lastVisiblePos == (totalItemCount - 1) && isSlidingToLast && (mCurrentPageNo ==mTopic.getTotalPageNo())) {
              clearLoadingHints();
            }
            else if((!isSlidingToLast)||  (isSlidingToLast&&(lastVisiblePos < (totalItemCount - 1)))) {
              TextView mIndexView = (TextView) (manager.findViewByPosition(lastVisiblePos)).findViewById(R.id.post_index);
              String temp = mIndexView.getText().toString();
               int index =0;
              if (temp.equals("??????")) {
                mIndex = 0;
              } else {
                String newTemp = temp.replaceAll("???", "");
                temp = newTemp.replaceAll("???", "");
                mIndex = Integer.parseInt(temp);
              }
              mCurrentPageNo = mIndex / POST_PER_PAGE + 1;
             //mTotalPageNo = mTopic.getTotalPageNo();
              String title = String.format("[%d/%d] %s", mCurrentPageNo, mTotalPageNo, mTopic.getTitle());
              mTitle.setText(title);
              mPageNo.setText(String.format("%d", mCurrentPageNo));
              mCurrentReadPageNo = mCurrentPageNo;
              //recyclerView.getAdapter().notifyDataSetChanged();
             // recyclerView.getAdapter().notifyItemRangeChanged(1,1,"x");
              //recyclerView.getAdapter().notifyItemRangeChanged(0,1);
            }
          }
        }
      }
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
          super.onScrolled(recyclerView, dx, dy);
          if(dy > 0){
            isSlidingToLast = true;
          }
      }
    });



    if (mTopic == null || !mTopic.getTopicID().equals(topic.getTopicID()) || PostListContent.POSTS.size() == 0) {
      // new topic, different topic, or no post loaded
      mTopic = topic;
      mFilterUser = null;
      reloadPostList();
      setTitle(mTopic.getBoardChsName() + " - ????????????");
    }
    else //Add by Vinney for recording article address
    {
        mTopic = topic;
        mFilterUser = null;
      setTitle(mTopic.getBoardChsName() + " - ????????????");
      if(!Settings.getInstance().isOpenTopicAdd()) {
        reloadPostList();
      }
      else {
        String title = String.format("[%d/%d] %s", mCurrentReadPageNo, mTotalPageNo, mTopic.getTitle());
        mTitle.setText(title);
        scrollToPosition();
        mCurrentPageNo =mCurrentReadPageNo;
        clearLoadingHints();
      }
    }
  }

  public void initPostNavigationButtons() {
    int alphaValue = 50;

    ImageButton imageButton;
    imageButton = (ImageButton) findViewById(R.id.post_list_action_top);
    imageButton.setImageAlpha(alphaValue);
    imageButton.setOnClickListener(this);

    imageButton = (ImageButton) findViewById(R.id.post_list_action_up);
    imageButton.setImageAlpha(alphaValue);
    imageButton.setOnClickListener(this);

    imageButton = (ImageButton) findViewById(R.id.post_list_action_down);
    imageButton.setImageAlpha(alphaValue);
    imageButton.setOnClickListener(this);

    imageButton = (ImageButton) findViewById(R.id.post_list_action_bottom);
    imageButton.setImageAlpha(alphaValue);
    imageButton.setOnClickListener(this);
  }

  public void clearLoadingHints() {
    dismissProgress();

    if (mRefreshLayout.isRefreshing()) {
      mRefreshLayout.finishRefresh(100);
    }
    if (mRefreshLayout.isLoading()) {
      mRefreshLayout.finishLoadMore(100);
    }
  }

  public void InsertPostListWithoutAlert() {
    //PostListContent.clear();
    //mRecyclerView.getAdapter().notifyDataSetChanged();
    if(mCurrentPageNo != 1) {
      mCurrentPageNo = mCurrentPageNo-1;
      loadPostListByPagesNew();
    }
    else {
      //  loadPostListByPages();
      clearLoadingHints();
      Toast.makeText(PostListActivity.this, "????????????", Toast.LENGTH_LONG).show();
    }
  }

  public void reloadPostListWithoutAlertNew() {
    //PostListContent.clear();
    //mRecyclerView.getAdapter().notifyDataSetChanged();
    //Two scenarios here:
    //Case 1: if already on last item then check next page
    //case 2: if not yet on the last item of this page. then check this page.
    String temp = PostListContent.POSTS.get(PostListContent.POSTS.size()-1).getPosition();

    int Index =0;
    if (temp.equals("??????")) {
      Index = 0;
    } else {
      String newTemp = temp.replaceAll("???", "");
      temp = newTemp.replaceAll("???", "");
      Index = Integer.parseInt(temp);
    }
    int tmpIndex = Index % POST_PER_PAGE;

    if (mCurrentPageNo == mTopic.getTotalPageNo() && tmpIndex<9 ) {
      loadnextpost();
    } else if( mCurrentPageNo == mTopic.getTotalPageNo() && tmpIndex==9)  {
      mCurrentPageNo += 1;
      loadnextpost();
    }
    else
    {
      clearLoadingHints();
    }
  }

  public void reloadPostListWithoutAlert() {
      PostListContent.clear();
      mRecyclerView.getAdapter().notifyDataSetChanged();
      loadPostListByPages();
  }

  public void reloadPostList() {
    showProgress("???????????????, ?????????...");

    reloadPostListWithoutAlert();
  }

  public void loadnextpost() {
    final SMTHHelper helper = SMTHHelper.getInstance();

    helper.wService.getPostListByPage(mTopic.getTopicURL(), mTopic.getTopicID(), mCurrentPageNo, mFilterUser)
            .flatMap(new Function<ResponseBody, Observable<Post>>() {
              @Override public Observable<Post> apply(@NonNull ResponseBody responseBody) throws Exception {
                try {
                  String response = responseBody.string();

                  List<Post> posts = SMTHHelper.ParsePostListFromWWW(response, mTopic);
                  if(posts.size()==0) {
                    return Observable.empty(); //handle error case
                  }
                  if(SMTHApplication.ReadRec == false) {
                    SMTHApplication.ReadPostFirst = posts.get(0);
                    SMTHApplication.ReadRec=true;
                  }
                  return Observable.fromIterable(posts);
                } catch (Exception e) {
                  SMTHApplication.ReadRec=false;
                  SMTHApplication.ReadPostFirst=null;
                  Log.e(TAG, Log.getStackTraceString(e));
                }
                return null;
              }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Post>() {
              @Override public void onSubscribe(@NonNull Disposable disposable) {

              }

              @Override public void onNext(@NonNull Post post) {

                String temp = PostListContent.POSTS.get(PostListContent.POSTS.size()-1).getPosition();
                int Index =0;
                if (temp.equals("??????")) {
                  Index = 0;
                } else {
                  String newTemp = temp.replaceAll("???", "");
                  temp = newTemp.replaceAll("???", "");
                  Index = Integer.parseInt(temp);
                }

                // Log.d(TAG, post.toString());
                 temp = post.getPosition();
                int mIndex =0;
                if (temp.equals("??????")) {
                  mIndex = 0;
                } else {
                  String newTemp = temp.replaceAll("???", "");
                  temp = newTemp.replaceAll("???", "");
                  mIndex = Integer.parseInt(temp);
                }
                if(mIndex > Index)
                {
                PostListContent.addItem(post);
               // mRecyclerView.getAdapter().notifyItemInserted(PostListContent.POSTS.size()-1);
                  mRecyclerView.getAdapter().notifyItemInserted(mIndex);
              }
              }

              @Override public void onError(@NonNull Throwable e) {
                clearLoadingHints();
                Toast.makeText(SMTHApplication.getAppContext(), "???????????????\n" + e.toString(), Toast.LENGTH_LONG).show();
              }

              @Override public void onComplete() {
                String temp = PostListContent.POSTS.get(PostListContent.POSTS.size()-1).getPosition();
                int Index =0;
                if (temp.equals("??????")) {
                  Index = 0;
                } else {
                  String newTemp = temp.replaceAll("???", "");
                  temp = newTemp.replaceAll("???", "");
                  Index = Integer.parseInt(temp);
                }
                if(Index == mTotalPageNo*POST_PER_PAGE-1) {
                  mCurrentPageNo -= 1;
                  Toast.makeText(SMTHApplication.getAppContext(),"???????????????",Toast.LENGTH_SHORT).show();
                }
                else if(Index >= mTotalPageNo*POST_PER_PAGE) {
                  mTotalPageNo += 1;
                  mTopic.setTotalPageNo(mTotalPageNo);
                  Toast.makeText(SMTHApplication.getAppContext(),"???????????????",Toast.LENGTH_SHORT).show();
                }
                  String title = String.format("[%d/%d] %s", mCurrentPageNo, mTotalPageNo, mTopic.getTitle());
                  mTitle.setText(title);
                  mPageNo.setText(String.format("%d", mCurrentPageNo));
                  mCurrentReadPageNo = mCurrentPageNo;
                 //mRecyclerView.getAdapter().notifyItemInserted(PostListContent.POSTS.size()-1);
                clearLoadingHints();

                  //Special User OFFLINE case: [] or [Category ?????????:]
                  if (PostListContent.POSTS.size() == 0) {
                    //Toast.makeText(SMTHApplication.getAppContext(),"???????????????-"+ PostListContent.POSTS.size()+"-!",Toast.LENGTH_LONG).show();
                    PostListContent.clear();
                    try {
                      Thread.sleep(1000);
                      Settings.getInstance().setUserOnline(false); //User Offline
                      onBackPressed();
                    } catch (InterruptedException e) {
                      e.printStackTrace();
                    }
                  }
              }
            });
  }


  public void loadPostListByPages() {
    final SMTHHelper helper = SMTHHelper.getInstance();

    helper.wService.getPostListByPage(mTopic.getTopicURL(), mTopic.getTopicID(), mCurrentPageNo, mFilterUser)
        .flatMap(new Function<ResponseBody, Observable<Post>>() {
          @Override public Observable<Post> apply(@NonNull ResponseBody responseBody) throws Exception {
            try {
              String response = responseBody.string();
              List<Post> posts = SMTHHelper.ParsePostListFromWWW(response, mTopic);
              if(posts.size()==0) {
                return Observable.empty(); //handle error case
              }
              if(SMTHApplication.ReadRec == false) {
                SMTHApplication.ReadPostFirst = posts.get(0);
                SMTHApplication.ReadRec=true;
              }
              return Observable.fromIterable(posts);
            } catch (Exception e) {
              SMTHApplication.ReadRec=false;
              SMTHApplication.ReadPostFirst=null;
              Log.e(TAG, Log.getStackTraceString(e));
            }
            return null;
          }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Post>() {
          @Override public void onSubscribe(@NonNull Disposable disposable) {

          }

          @Override public void onNext(@NonNull Post post) {
            // Log.d(TAG, post.toString());
            PostListContent.addItem(post);
            mRecyclerView.getAdapter().notifyItemInserted(PostListContent.POSTS.size() - 1);
          }

          @Override public void onError(@NonNull Throwable e) {
            clearLoadingHints();
            Toast.makeText(SMTHApplication.getAppContext(), "???????????????\n" + e.toString(), Toast.LENGTH_LONG).show();
          }

          @Override public void onComplete() {
            mTotalPageNo = mTopic.getTotalPageNo();
            String title = String.format("[%d/%d] %s", mCurrentPageNo, mTopic.getTotalPageNo(), mTopic.getTitle());
            mTitle.setText(title);
            mPageNo.setText(String.format("%d", mCurrentPageNo));
            mCurrentReadPageNo = mCurrentPageNo;
            clearLoadingHints();
            SMTHApplication.deletionCount++;

            //Special User OFFLINE case: [] or [Category ?????????:]
            if(PostListContent.POSTS.size() == 0) {
                //Toast.makeText(SMTHApplication.getAppContext(),"???????????????-"+ PostListContent.POSTS.size()+"-!",Toast.LENGTH_LONG).show();
                PostListContent.clear();
                try {
                  Thread.sleep(1000);
                  Settings.getInstance().setUserOnline(false); //User Offline
                  onBackPressed();
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
            }
          }
        });
  }

  public void loadPostListByPagesNew() {
    final SMTHHelper helper = SMTHHelper.getInstance();

    helper.wService.getPostListByPage(mTopic.getTopicURL(), mTopic.getTopicID(), mCurrentPageNo, mFilterUser)
            .flatMap(new Function<ResponseBody, Observable<Post>>() {
              @Override public Observable<Post> apply(@NonNull ResponseBody responseBody) throws Exception {
                try {
                  String response = responseBody.string();
                  List<Post> posts = SMTHHelper.ParsePostListFromWWW(response, mTopic);
                  if(posts.size()==0) {
                    return Observable.empty(); //handle error case
                  }
                  if(SMTHApplication.ReadRec == false) {
                    SMTHApplication.ReadPostFirst = posts.get(0);
                    SMTHApplication.ReadRec=true;
                  }
                  return Observable.fromIterable(posts);
                } catch (Exception e) {
                  SMTHApplication.ReadRec=false;
                  SMTHApplication.ReadPostFirst=null;
                  Log.e(TAG, Log.getStackTraceString(e));
                }
                return null;
              }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<Post>() {
              @Override public void onSubscribe(@NonNull Disposable disposable) {

              }

              @Override public void onNext(@NonNull Post post) {
                String temp = post.getPosition();
                int Index =0;
                if (temp.equals("??????")) {
                  Index = 0;
                } else {
                  String newTemp = temp.replaceAll("???", "");
                  temp = newTemp.replaceAll("???", "");
                  Index = Integer.parseInt(temp);
                }
                Index = Index % POST_PER_PAGE;
                //PostListContent.addItem(Index,post);
                PostListContent.InsertItem(Index,post);
                mRecyclerView.getAdapter().notifyItemInserted(Index);
              }

              @Override public void onError(@NonNull Throwable e) {
                clearLoadingHints();
                Toast.makeText(SMTHApplication.getAppContext(), "???????????????\n" + e.toString(), Toast.LENGTH_LONG).show();
              }

              @Override public void onComplete() {
                mTotalPageNo = mTopic.getTotalPageNo();
                String title = String.format("[%d/%d] %s", mCurrentPageNo, mTopic.getTotalPageNo(), mTopic.getTitle());
                mTitle.setText(title);
                mPageNo.setText(String.format("%d", mCurrentPageNo));
                mCurrentReadPageNo = mCurrentPageNo;
                clearLoadingHints();

                //Special User OFFLINE case: [] or [Category ?????????:]
                if(PostListContent.POSTS.size() == 0)
                {
                  //Toast.makeText(SMTHApplication.getAppContext(),"???????????????-"+ PostListContent.POSTS.size()+"-!",Toast.LENGTH_LONG).show();
                  PostListContent.clear();

                  try {
                    Thread.sleep(1000);
                    Settings.getInstance().setUserOnline(false); //User Offline
                    onBackPressed();
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                }
              }
            });
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // http://stackoverflow.com/questions/10692755/how-do-i-hide-a-menu-item-in-the-actionbar
    getMenuInflater().inflate(R.menu.post_list_menu, menu);

    MenuItem item = menu.findItem(R.id.post_list_action_enter_board);
    if (SMTHApplication.FROM_BOARD_BOARD.equals(mFrom)) {
      // from BoardTopicActivity
      item.setVisible(false);
    } else if (SMTHApplication.FROM_BOARD_HOT.equals(mFrom)) {
      // from HotTopicFragment
      item.setVisible(true);
    }
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == android.R.id.home) {
      // This ID represents the Home or Up button. In the case of this
      // activity, the Up button is shown. Use NavUtils to allow users
      // to navigate up one level in the application structure. For
      // more details, see the Navigation pattern on Android Design:
      //
      // http://developer.android.com/design/patterns/navigation.html#up-vs-back
      //
      onBackPressed();
      return true;
    } else if (id == R.id.post_list_action_refresh) {
      reloadPostList();
    } else if (id == R.id.post_list_action_enter_board) {
      Board board = new Board();
      board.initAsBoard(mTopic.getBoardChsName(),mTopic.getBoardEngName(), "", "");
      Intent intent = new Intent(this, BoardTopicActivity.class);
      intent.putExtra(SMTHApplication.BOARD_OBJECT, (Parcelable) board);
      startActivity(intent);
    }
    return super.onOptionsItemSelected(item);
  }
  @Override public void onBackPressed() {

    super.onBackPressed();

    if(SMTHApplication.isValidUser()&&!Settings.getInstance().isUserOnline() && !SMTHApplication.deletionPost) {
        if(SMTHApplication.deletionCount < 2) {
          Intent intent = new Intent(PostListActivity.this, LoginActivity.class);
          startActivityForResult(intent, MainActivity.LOGIN_ACTIVITY_REQUEST_CODE);
        }else {
          SMTHApplication.deletionCount = 0;
          BoardTopicActivity.getInstance().RefreshBoardTopicFromPageOne();
        }

    } else if(SMTHApplication.deletionPost)
    {

      if (mRecyclerView.isComputingLayout()) {
        mRecyclerView.post(new Runnable() {
          @Override
          public void run() {
            reloadPostList();
          }
        });
      } else {
        reloadPostList();
      }
      SMTHApplication.deletionPost = false;
    }

  }

  @SuppressLint("NonConstantResourceId")
  @Override public void onClick(View v) {
    // page navigation buttons
    switch (v.getId()) {
      case R.id.post_list_first_page:
        if(Settings.getInstance().isautoloadmore()) {
          if (mCurrentPageNo == 1) {
            Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
          } else {
            mCurrentPageNo = 1;
            reloadPostList();
          }
        }
        else
        {
          mCurrentPageNo = 1;
          reloadPostList();
        }
        break;
      case R.id.post_list_pre_page:
        if(Settings.getInstance().isautoloadmore()) {
          if (mCurrentPageNo == 1) {
            Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
          } else {
            mCurrentPageNo -= 1;
            reloadPostList();
          }
        }
        else{
          if (mCurrentPageNo == 1) {
            reloadPostList();
          } else {
            mCurrentPageNo -= 1;
            reloadPostList();
          }
        }
        break;
      case R.id.post_list_next_page:
        goToNextPage();
        break;
      case R.id.post_list_last_page:
        if(Settings.getInstance().isautoloadmore()) {
          //Change by Vinney
          if (mCurrentPageNo == mTopic.getTotalPageNo() || mCurrentReadPageNo == mTotalPageNo) {
            Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
          } else {
            //mCurrentPageNo = mTopic.getTotalPageNo();
            mCurrentPageNo = mTotalPageNo;
            reloadPostList();
          }
        }
        else {
            mCurrentPageNo = mTotalPageNo;
            reloadPostList();
          }
        break;
      case R.id.post_list_go_page:
        int pageNo;
        pageNo = Integer.parseInt(mPageNo.getText().toString());
        if(Settings.getInstance().isautoloadmore()) {
          try {
            if (mCurrentPageNo == pageNo) {
              Toast.makeText(PostListActivity.this, String.format("?????????%d??????", pageNo), Toast.LENGTH_SHORT).show();
            } else if (pageNo >= 1 && pageNo <= mTopic.getTotalPageNo()) {
              mCurrentPageNo = pageNo;
              // turn off keyboard
              mPageNo.clearFocus();
              InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
              im.hideSoftInputFromWindow(mPageNo.getWindowToken(), 0);
              // jump now
              reloadPostList();
            } else {
              Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
            }
          } catch (Exception e) {
            Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
          }
        }
        else        {
          try {
            if (mCurrentPageNo == pageNo) {
              reloadPostList();
            } else if (pageNo >= 1 && pageNo <= mTopic.getTotalPageNo()) {
              mCurrentPageNo = pageNo;
              // turn off keyboard
              mPageNo.clearFocus();
              InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
              im.hideSoftInputFromWindow(mPageNo.getWindowToken(), 0);
              // jump now
              reloadPostList();
            } else {
              Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
            }
          } catch (Exception e) {
            Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
          }
        }
        break;
      case R.id.post_list_action_top:
        mRecyclerView.scrollToPosition(0);
        break;
      case R.id.post_list_action_up:
        int prevPos = linearLayoutManager.findFirstVisibleItemPosition() - 1;
        if (prevPos >= 0) {
          mRecyclerView.smoothScrollToPosition(prevPos);
        }
        break;
      case R.id.post_list_action_down:
        int nextPos = linearLayoutManager.findLastVisibleItemPosition() + 1;
        if (nextPos < mRecyclerView.getAdapter().getItemCount()) {
          mRecyclerView.smoothScrollToPosition(nextPos);
        }
        break;
      case R.id.post_list_action_bottom:
        mRecyclerView.scrollToPosition(mRecyclerView.getAdapter().getItemCount() - 1);
        break;

    }
  }

  public void goToNextPage() {
    if(Settings.getInstance().isautoloadmore()) {
      if (mCurrentPageNo == mTopic.getTotalPageNo()) {
        Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
        clearLoadingHints();
      } else {
        mCurrentPageNo += 1;
        reloadPostList();
      }
    }
    else{
      if (mCurrentPageNo == mTopic.getTotalPageNo()) {
        reloadPostList();
      } else if(mCurrentPageNo < mTopic.getTotalPageNo()) {
        mCurrentPageNo += 1;
        reloadPostList();
      }
    }
  }


  public void LoadMoreItems() {

    if (mCurrentPageNo == mTopic.getTotalPageNo()) {
      Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
      clearLoadingHints();
    } else {
      synchronized (this) {

        mCurrentPageNo += 1;
        //reloadPostList();
       // showProgress("???????????????, ?????????...");
        loadPostListByPages();
      }
    }
  }

  @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      RecyclerViewUtil.ScrollRecyclerViewByKey(mRecyclerView, keyCode);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  // http://stackoverflow.com/questions/4500354/control-volume-keys
  @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
    // disable the beep sound when volume up/down is pressed
    if (Settings.getInstance().isVolumeKeyScroll() && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }


  @Override public boolean onItemLongClicked(final int position, View v) {
    if (position == RecyclerView.NO_POSITION || position >= PostListContent.POSTS.size()) return false;

    //Log.d(TAG, String.format("Post by %s is long clicked", PostListContent.POSTS.get(position).getAuthor()));

    final PostActionAlertDialogItem[] menuItems = {
        new PostActionAlertDialogItem(getString(R.string.post_reply_post), R.drawable.ic_reply_black_48dp),       // 0
        new PostActionAlertDialogItem(getString(R.string.post_like_post), R.drawable.like_black),       // 1
        new PostActionAlertDialogItem(getString(R.string.post_reply_mail), R.drawable.ic_email_black_48dp),    // 2
        new PostActionAlertDialogItem(getString(R.string.post_query_author), R.drawable.ic_person_black_48dp),    // 3
        new PostActionAlertDialogItem(getString(R.string.post_filter_author), R.drawable.ic_find_in_page_black_48dp),    // 4
        new PostActionAlertDialogItem(getString(R.string.post_copy_content), R.drawable.ic_content_copy_black_48dp),    // 5
        new PostActionAlertDialogItem(getString(R.string.post_foward), R.drawable.ic_send_black_48dp),     // 6
        new PostActionAlertDialogItem(getString(R.string.post_view_in_browser), R.drawable.ic_open_in_browser_black_48dp), // 7
        new PostActionAlertDialogItem(getString(R.string.post_share), R.drawable.ic_share_black_48dp), // 8
        new PostActionAlertDialogItem(getString(R.string.post_delete_post), R.drawable.ic_delete_black_48dp), // 9
        new PostActionAlertDialogItem(getString(R.string.post_edit_post), R.drawable.ic_edit_black_48dp), // 10
        new PostActionAlertDialogItem(getString(R.string.post_convert_image), R.drawable.ic_photo_black_48dp), // 11
        //new PostActionAlertDialogItem(getString(R.string.post_reply_author),R.drawable.ic_expand_less_36dp) // 11
        new PostActionAlertDialogItem(getString(R.string.post_reply_author),R.drawable.ic_reply_black_48dp) // 11
        //Vinney add reply head author
    };

    ListAdapter adapter = new ArrayAdapter<PostActionAlertDialogItem>(getApplicationContext(), R.layout.post_popup_menu_item, menuItems) {
      ViewHolder holder;

      public View getView(int position, View convertView, ViewGroup parent) {
        final LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
          convertView = inflater.inflate(R.layout.post_popup_menu_item, null);

          holder = new ViewHolder();
          holder.mIcon = (ImageView) convertView.findViewById(R.id.post_popupmenu_icon);
          holder.mTitle = (TextView) convertView.findViewById(R.id.post_popupmenu_title);
          convertView.setTag(holder);
        } else {
          // view already defined, retrieve view holder
          holder = (ViewHolder) convertView.getTag();
        }

        holder.mTitle.setText(menuItems[position].text);
        holder.mIcon.setImageResource(menuItems[position].icon);
        return convertView;
      }

      class ViewHolder {
        ImageView mIcon;
        TextView mTitle;
      }
    };

    AlertDialog dialog = new AlertDialog.Builder(this).setTitle(getString(R.string.post_alert_title))
        .setAdapter(adapter, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            onPostPopupMenuItem(position, which);
          }
        })
        .create();
    dialog.setCanceledOnTouchOutside(true);
    dialog.setCancelable(true);

    dialog.show();
    return true;
  }

   public boolean onItemLeftClicked(final int position, View v) {
     // post_reply_mail
     // Toast.makeText(PostListActivity.this, "?????????????????????:TBD", Toast.LENGTH_SHORT).show();
     if (position >= PostListContent.POSTS.size()) {
       Log.e(TAG, "onItemRightClicked: " + "Invalid Post index" + position);
       return false;
     }

     Post post = PostListContent.POSTS.get(position);
     ComposePostContext postContext = new ComposePostContext();
     postContext.setBoardEngName(mTopic.getBoardEngName());
     postContext.setPostId(post.getPostID());
     postContext.setPostTitle(mTopic.getTitle());
     postContext.setPostAuthor(post.getRawAuthor());
     postContext.setPostContent(post.getRawContent());
     postContext.setComposingMode(ComposePostContext.MODE_REPLY_MAIL);

     Intent intent = new Intent(this, ComposePostActivity.class);
     intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
     startActivity(intent);
    return true;
  }
   public boolean onItemRightClicked(final int position, View v) {
     // post_reply_post
     if (position >= PostListContent.POSTS.size()) {
       Log.e(TAG, "onItemRightClicked: " + "Invalid Post index" + position);
       return false;
     }

     Post post = PostListContent.POSTS.get(position);
     ComposePostContext postContext = new ComposePostContext();
     postContext.setBoardEngName(mTopic.getBoardEngName());
     postContext.setPostId(post.getPostID());
     postContext.setPostTitle(mTopic.getTitle());
     postContext.setPostAuthor(post.getRawAuthor());
     postContext.setPostContent(post.getRawContent());
     postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);

     Intent intent = new Intent(this, ComposePostActivity.class);
     intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
     startActivityForResult(intent, ComposePostActivity.COMPOSE_ACTIVITY_REQUEST_CODE);
    return true;
  }

  public boolean onItemBottomClicked(final int position, View v) {
    //goToNextPage();
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();

        //LastItemPosition
    int lastVisiblePos = manager.findLastVisibleItemPosition();
    int totalItemCount = manager.getItemCount();
    if(lastVisiblePos <= totalItemCount-1 )
    mRecyclerView.scrollToPosition(lastVisiblePos+1);

    return true;
  }

  public boolean onItemTopClicked(final int position, View v) {
    LinearLayoutManager manager = (LinearLayoutManager) mRecyclerView.getLayoutManager();

    //LastItemPosition
    int FirstVisiblePos = manager.findFirstVisibleItemPosition();
    if(FirstVisiblePos > 1 )
      mRecyclerView.scrollToPosition(FirstVisiblePos-1);
    return true;
  }
  private void onPostPopupMenuItem(int position, int which) {
    //        Log.d(TAG, String.format("MenuItem %d was clicked", which));
    if (position >= PostListContent.POSTS.size()) {
      Log.e(TAG, "onPostPopupMenuItem: " + "Invalid Post index" + position);
      return;
    }

    Post post = PostListContent.POSTS.get(position);

    if (which == 0) {
      // post_reply_post
      ComposePostContext postContext = new ComposePostContext();
      postContext.setBoardEngName(mTopic.getBoardEngName());
      postContext.setPostId(post.getPostID());
      postContext.setPostTitle(mTopic.getTitle());
      postContext.setPostAuthor(post.getRawAuthor());
      postContext.setPostContent(post.getRawContent());
      postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);

      Intent intent = new Intent(this, ComposePostActivity.class);
      intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
      startActivityForResult(intent, ComposePostActivity.COMPOSE_ACTIVITY_REQUEST_CODE);
    } else if (which == 1) {
      // like
      // Toast.makeText(PostListActivity.this, "Like:TBD", Toast.LENGTH_SHORT).show();
      PopupLikeWindow popup = new PopupLikeWindow();
      popup.initPopupWindow(this);
      popup.showAtLocation(mRecyclerView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 100);
    } else if (which == 2) {
      // post_reply_mail
      // Toast.makeText(PostListActivity.this, "?????????????????????:TBD", Toast.LENGTH_SHORT).show();
      ComposePostContext postContext = new ComposePostContext();
      postContext.setBoardEngName(mTopic.getBoardEngName());
      postContext.setPostId(post.getPostID());
      postContext.setPostTitle(mTopic.getTitle());
      postContext.setPostAuthor(post.getRawAuthor());
      postContext.setPostContent(post.getRawContent());
      postContext.setComposingMode(ComposePostContext.MODE_REPLY_MAIL);

      Intent intent = new Intent(this, ComposePostActivity.class);
      intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
      startActivity(intent);
    } else if (which == 3) {
      // post_query_author
      Intent intent = new Intent(this, QueryUserActivity.class);
      intent.putExtra(SMTHApplication.QUERY_USER_INFO, post.getRawAuthor());
      startActivity(intent);
    } else if (which == 4) {
      // read posts from current users only
      if (mFilterUser == null) {
        Toast.makeText(PostListActivity.this, "?????????ID! ?????????????????????????????????.", Toast.LENGTH_SHORT).show();
        mFilterUser = post.getRawAuthor();
      } else {
        Toast.makeText(PostListActivity.this, "??????????????????!", Toast.LENGTH_SHORT).show();
        mFilterUser = null;
      }
      mCurrentPageNo = 1;
      reloadPostList();
    } else if (which == 5) {
      // copy post content
      // http://stackoverflow.com/questions/8056838/dealing-with-deprecated-android-text-clipboardmanager
      String content;
      if (post != null) {
        content = post.getRawContent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          final android.content.ClipboardManager clipboardManager =
              (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          final android.content.ClipData clipData = android.content.ClipData.newPlainText("PostContent", content);
          clipboardManager.setPrimaryClip(clipData);
        } else {
          final android.text.ClipboardManager clipboardManager =
              (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
          clipboardManager.setText(content);
        }
        Toast.makeText(PostListActivity.this, "?????????????????????????????????", Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(PostListActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
      }
    } else if (which == 6) {
      // post_foward_self
      // Toast.makeText(PostListActivity.this, "????????????:TBD", Toast.LENGTH_SHORT).show();
      PopupForwardWindow popup = new PopupForwardWindow();
      popup.initPopupWindow(this, post);
      popup.showAtLocation(mRecyclerView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 100);
    } else if (which == 7) {
      // open post in browser
      String url = String.format("http://m.newsmth.net/article/%s/%s?p=%d", mTopic.getBoardEngName(), mTopic.getTopicID(), mCurrentPageNo);
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } else if (which == 8) {
      // post_share
      // Toast.makeText(PostListActivity.this, "??????:TBD", Toast.LENGTH_SHORT).show();
      sharePost(post);
    } else if (which == 9) {
      // delete post
      SMTHApplication.deletionPost = true;
      deletePost(post);
    } else if (which == 10) {
      // edit post
      ComposePostContext postContext = new ComposePostContext();
      postContext.setBoardEngName(mTopic.getBoardEngName());
      postContext.setPostId(post.getPostID());
      postContext.setPostTitle(mTopic.getTitle());
      postContext.setPostAuthor(post.getRawAuthor());
      postContext.setPostContent(post.getRawContent());
      postContext.setComposingMode(ComposePostContext.MODE_EDIT_POST);

      Intent intent = new Intent(this, ComposePostActivity.class);
      intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
      startActivity(intent);
    } else if (which == 11) {
      // generate screenshot of current post
      View v = mRecyclerView.getLayoutManager().findViewByPosition(position);

      // convert title + post to image
      captureView(mTitle, v, post.getPostID());
    }
    //Vinney
    else if (which == 12) {
      // post_reply_post
      ComposePostContext postContext = new ComposePostContext();
      postContext.setBoardEngName(mTopic.getBoardEngName());
      postContext.setPostId(SMTHApplication.ReadPostFirst.getPostID());
      postContext.setPostTitle(mTopic.getTitle());

      postContext.setPostAuthor(SMTHApplication.ReadPostFirst.getRawAuthor());
      postContext.setPostContent(SMTHApplication.ReadPostFirst.getRawContent());

      postContext.setComposingMode(ComposePostContext.MODE_REPLY_POST);

      Intent intent = new Intent(this, ComposePostActivity.class);
      intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
      startActivityForResult(intent, ComposePostActivity.COMPOSE_ACTIVITY_REQUEST_CODE);
    }

  }

  public void captureView(View v1, View v2, String postID){
    //Create a Bitmap with the same dimensions
    Bitmap image = Bitmap.createBitmap(v1.getWidth(), v1.getHeight() + v2.getHeight(), Bitmap.Config.RGB_565);
    //Draw the view inside the Bitmap
    Canvas canvas = new Canvas(image);

    if(Settings.getInstance().isNightMode()) {
      canvas.drawColor(Color.BLACK);
    } else {
      canvas.drawColor(Color.WHITE);
    }
    v1.draw(canvas);
    canvas.translate(0, v1.getHeight());
    v2.draw(canvas);
    canvas.save();

    // save image to sdcard
    try {
      if (TextUtils.equals(Environment.getExternalStorageState(), Environment.MEDIA_MOUNTED)) {
        String path = Environment.getExternalStorageDirectory().getPath() + "/zSMTH/";
        File dir = new File(path);
        if (!dir.exists()) {
          dir.mkdirs();
        }

        String IMAGE_FILE_PREFIX = "post-";
        String IMAGE_FILE_SUFFIX = ".jpg";
        File outFile = new File(dir, IMAGE_FILE_PREFIX + postID + IMAGE_FILE_SUFFIX);
        FileOutputStream out = new FileOutputStream(outFile);

        image.compress(Bitmap.CompressFormat.JPEG, 90, out); //Output
        Toast.makeText(PostListActivity.this, "???????????????: /zSMTH/" + outFile.getName(), Toast.LENGTH_SHORT).show();

        // make sure the new file can be recognized soon
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)));
      }
    } catch (Exception e) {
      Log.e(TAG, "saveImageToFile: " + Log.getStackTraceString(e));
      Toast.makeText(PostListActivity.this, "??????????????????:\n" + e.toString(), Toast.LENGTH_LONG).show();
    }
  }

  public void deletePost(Post post) {
    SMTHHelper helper = SMTHHelper.getInstance();

    helper.wService.deletePost(mTopic.getBoardEngName(), post.getPostID()).map(new Function<ResponseBody, String>() {
      @Override public String apply(@NonNull ResponseBody responseBody) throws Exception {
        try {
          String response = SMTHHelper.DecodeResponseFromWWW(responseBody.bytes());
          return SMTHHelper.parseDeleteResponse(response);
        } catch (Exception e) {
          Log.e(TAG, "call: " + Log.getStackTraceString(e));
        }
        return null;
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
      @Override public void onSubscribe(@NonNull Disposable disposable) {

      }

      @Override public void onNext(@NonNull String s) {
          Toast.makeText(PostListActivity.this, s, Toast.LENGTH_LONG).show();
      }

      @Override public void onError(@NonNull Throwable e) {
        Toast.makeText(PostListActivity.this, "?????????????????????\n" + e.toString(), Toast.LENGTH_LONG).show();

      }

      @Override public void onComplete() {
        //Vinney??????????????????????????????????????????????????????????????????????????????board??????????????????????????????
          mCurrentPageNo = 1;
          mCurrentReadPageNo = 1;
          reloadPostListWithoutAlert();
      }
    });
  }

  public void sharePost(Post post) {
    OnekeyShare oks = new OnekeyShare();
    //??????sso??????
    oks.disableSSOWhenAuthorize();

    // prepare information from the post
    String title = String.format("[%s] %s @ ????????????", mTopic.getBoardChsName(), mTopic.getTitle());
    String postURL =
        String.format("http://m.newsmth.net/article/%s/%s?p=%d", mTopic.getBoardEngName(), mTopic.getTopicID(), mCurrentPageNo);
    String content = String.format("[%s]??????????????????: %s", post.getAuthor(), post.getRawContent());
    // the max length of webo is 140
    if (content.length() > 110) {
      content = content.substring(0, 110);
    }
    content += String.format("...\nLink:%s", postURL);

    // default: use zSMTH logo
    String imageURL = "http://zsmth-android.zfdang.com/zsmth.png";
    List<Attachment> attaches = post.getAttachFiles();
    if (attaches != null && attaches.size() > 0) {
      // use the first attached image
      imageURL = attaches.get(0).getResizedImageSource();
    }

    // more information about OnekeyShare
    // http://wiki.mob.com/docs/sharesdk/android/cn/sharesdk/onekeyshare/OnekeyShare.html

    // title???????????????????????????????????????????????????????????????QQ???QQ????????????
    oks.setTitle(title);

    // titleUrl?????????????????????????????????Linked-in,QQ???QQ????????????
    // oks.setTitleUrl("http://sharesdk.cn");

    // text???????????????????????????????????????????????????
    oks.setText(content);

    // ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    // imageUrl??????????????????????????????????????????????????????QQ?????????Linked-In???????????????
    oks.setImageUrl(imageURL);

    // imagePath???????????????????????????Linked-In?????????????????????????????????
    //oks.setImagePath("/sdcard/test.jpg");//??????SDcard????????????????????????

    // url???????????????????????????????????????????????????
    oks.setUrl(postURL);

    // comment???????????????????????????????????????????????????QQ????????????
    //        oks.setComment("????????????????????????");
    // site??????????????????????????????????????????QQ????????????
    //        oks.setSite("ShareSDK");
    // siteUrl??????????????????????????????????????????QQ????????????
    //        oks.setSiteUrl("http://sharesdk.cn");

    // set callback functions
    oks.setCallback(new PlatformActionListener() {
      @Override public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
        Toast.makeText(PostListActivity.this, "????????????!", Toast.LENGTH_SHORT).show();
      }

      @Override public void onError(Platform platform, int i, Throwable throwable) {
        Toast.makeText(PostListActivity.this, "????????????:\n" + throwable.toString(), Toast.LENGTH_LONG).show();
      }

      @Override public void onCancel(Platform platform, int i) {
      }
    });

    // ????????????GUI
    oks.show(this);
  }

  @Override public boolean onTouch(View v, MotionEvent event) {
    mGestureDetector.onTouchEvent(event);
    return false;
  }

  @Override public void OnLikeAction(String score, String msg) {
    //        Log.d(TAG, "OnLikeAction: " + score + msg);

    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.addLike(mTopic.getBoardEngName(), mTopic.getTopicID(), score, msg, "")
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<AjaxResponse>() {
          @Override public void onSubscribe(@NonNull Disposable disposable) {

          }

          @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
            // Log.d(TAG, "onNext: " + ajaxResponse.toString());
            if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
              Toast.makeText(PostListActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
              reloadPostList();
            } else {
              Toast.makeText(PostListActivity.this, ajaxResponse.toString(), Toast.LENGTH_LONG).show();
            }
          }

          @Override public void onError(@NonNull Throwable e) {
            Toast.makeText(PostListActivity.this, "??????Like??????!\n" + e.toString(), Toast.LENGTH_LONG).show();
          }

          @Override public void onComplete() {

          }
        });
  }

  @Override public void OnForwardAction(Post post, String target, boolean threads, boolean noref, boolean noatt) {
    //        Log.d(TAG, "OnForwardAction: ");

    String strThreads = null;
    if (threads) strThreads = "on";
    String strNoref = null;
    if (noref) strNoref = "on";
    String strNoatt = null;
    if (noatt) strNoatt = "on";
    String strNoansi = null;
    if (target != null && target.contains("@")) strNoansi = "on";

    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.forwardPost(mTopic.getBoardEngName(), post.getPostID(), target, strThreads, strNoref, strNoatt, strNoansi)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<AjaxResponse>() {
          @Override public void onSubscribe(@NonNull Disposable disposable) {

          }

          @Override public void onNext(@NonNull AjaxResponse ajaxResponse) {
            // Log.d(TAG, "onNext: " + ajaxResponse.toString());
            if (ajaxResponse.getAjax_st() == AjaxResponse.AJAX_RESULT_OK) {
              Toast.makeText(PostListActivity.this, ajaxResponse.getAjax_msg(), Toast.LENGTH_SHORT).show();
            } else {
              Toast.makeText(PostListActivity.this, ajaxResponse.toString(), Toast.LENGTH_LONG).show();
            }
          }

          @Override public void onError(@NonNull Throwable e) {
            Toast.makeText(PostListActivity.this, "???????????????\n" + e.toString(), Toast.LENGTH_LONG).show();
          }

          @Override public void onComplete() {

          }
        });
  }

  @Override public void OnRePostAction(Post post, String target, String outgo) {
    SMTHHelper helper = SMTHHelper.getInstance();
    helper.wService.repostPost(mTopic.getBoardEngName(), post.getPostID(), target, outgo).map(new Function<ResponseBody, String>() {
      @Override public String apply(@NonNull ResponseBody responseBody) throws Exception {
        try {
          String response = SMTHHelper.DecodeResponseFromWWW(responseBody.bytes());
          return SMTHHelper.parseRepostResponse(response);
        } catch (Exception e) {
          Log.e(TAG, "call: " + Log.getStackTraceString(e));
        }
        return null;
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
      @Override public void onSubscribe(@NonNull Disposable disposable) {

      }

      @Override public void onNext(@NonNull String s) {
        Toast.makeText(SMTHApplication.getAppContext(), s, Toast.LENGTH_SHORT).show();
      }

      @Override public void onError(@NonNull Throwable e) {
        Toast.makeText(SMTHApplication.getAppContext(), e.toString(), Toast.LENGTH_LONG).show();
      }

      @Override public void onComplete() {

      }
    });
  }
}