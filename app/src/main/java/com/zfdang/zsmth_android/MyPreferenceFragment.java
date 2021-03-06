package com.zfdang.zsmth_android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.widget.Toast;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.zfdang.SMTHApplication;
import com.zfdang.zsmth_android.helpers.ActivityUtils;
import com.zfdang.zsmth_android.helpers.FileLess;
import com.zfdang.zsmth_android.helpers.FileSizeUtil;
import com.zfdang.zsmth_android.models.ComposePostContext;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.io.File;

/**
 * Created by zfdang on 2016-5-2.
 */
public class MyPreferenceFragment extends PreferenceFragmentCompat {
  private static final String TAG = "PreferenceFragment";
  Preference fresco_cache;
  Preference okhttp3_cache;

  CheckBoxPreference signature_control;
  Preference signature_content;

  CheckBoxPreference launch_bottom_navi;
  CheckBoxPreference launch_hottopic_as_entry;
  CheckBoxPreference open_topic_add;
  CheckBoxPreference diff_read_topic;
  CheckBoxPreference daynight_control;
  CheckBoxPreference setting_post_navigation_control;
  CheckBoxPreference auto_load_more;

  CheckBoxPreference setting_volume_key_scroll;
  ListPreference setting_fontsize_control;
  CheckBoxPreference image_quality_control;

  CheckBoxPreference notification_control_mail;
  CheckBoxPreference notification_control_like;
  CheckBoxPreference notification_control_reply;
  CheckBoxPreference notification_control_at;

  CheckBoxPreference topic_fwd_self; //Vinney
  CheckBoxPreference set_id_check; //Vinney

  CheckBoxPreference set_left_nav_slide;

  Preference app_feedback;
  Preference app_version;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addPreferencesFromResource(R.xml.preferences);

    fresco_cache = findPreference("setting_fresco_cache");
    fresco_cache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        // clear cache, then update cache size
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        imagePipeline.clearDiskCaches();

        updateFrescoCache();
        return true;
      }
    });

    okhttp3_cache = findPreference("setting_okhttp3_cache");
    okhttp3_cache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        // clear cache, then update cache size
        File cache = new File(SMTHApplication.getAppContext().getCacheDir(), "Responses");
        FileLess.$del(cache);
        if (!cache.exists()) {
          cache.mkdir();
        }

        updateOkHttp3Cache();
        return true;
      }
    });


    launch_bottom_navi = (CheckBoxPreference) findPreference("launch_bottom_navi");
    launch_bottom_navi.setChecked(Settings.getInstance().isLaunchBottomNavi());
    launch_bottom_navi.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isLaunchBottomNavi();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setLaunchBottomNavi(bValue);
        Activity activity = getActivity();
        if (activity != null) {
          Intent intent = new Intent(activity.getApplicationContext(), MainActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(intent);
          activity.finish();
        }
        return true;
      }
    });



    launch_hottopic_as_entry = (CheckBoxPreference) findPreference("launch_hottopic_as_entry");
    launch_hottopic_as_entry.setChecked(Settings.getInstance().isLaunchHotTopic());
    launch_hottopic_as_entry.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isLaunchHotTopic();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setLaunchHotTopic(bValue);
        return true;
      }
    });
	
	open_topic_add = (CheckBoxPreference) findPreference("open_topic_add");
    open_topic_add.setChecked(Settings.getInstance().isOpenTopicAdd());
    open_topic_add.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isOpenTopicAdd();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setOpenTopicAdd(bValue);
        return true;
      }
    });

    diff_read_topic = (CheckBoxPreference) findPreference("diff_read_topic");
    diff_read_topic.setChecked(Settings.getInstance().isDiffReadTopic());
    diff_read_topic.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bDiffRead = Settings.getInstance().isDiffReadTopic();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bDiffRead = boolVal;
        }
        Settings.getInstance().setDiffReadTopic(bDiffRead);
        SMTHApplication.ReadTopicLists.clear();// ?????????on??????off??????????????????????????????
        return true;
      }
    });

    setting_post_navigation_control = (CheckBoxPreference) findPreference("setting_post_navigation_control");
    setting_post_navigation_control.setChecked(Settings.getInstance().hasPostNavBar());
    setting_post_navigation_control.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().hasPostNavBar();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setPostNavBar(bValue);
        return true;
      }
    });

    auto_load_more = (CheckBoxPreference) findPreference("auto_load_more");
    auto_load_more.setChecked(Settings.getInstance().isautoloadmore());
    auto_load_more.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isautoloadmore();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().Setautoloadmore(bValue);
        return true;
      }
    });


    setting_volume_key_scroll = (CheckBoxPreference) findPreference("setting_volume_key_scroll");
    setting_volume_key_scroll.setChecked(Settings.getInstance().isVolumeKeyScroll());
    setting_volume_key_scroll.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isVolumeKeyScroll();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setVolumeKeyScroll(bValue);
        return true;
      }
    });

    setting_fontsize_control = (ListPreference) findPreference("setting_fontsize_control");
    setting_fontsize_control.setValueIndex(Settings.getInstance().getFontIndex());
    setting_fontsize_control.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        int fontIndex = Settings.getInstance().getFontIndex();
        if (newValue instanceof String) {
          fontIndex = Integer.parseInt((String) newValue);
        }
        Settings.getInstance().setFontIndex(fontIndex);

        // recreate activity for font size to take effect
        Activity activity = getActivity();
        if (activity != null) {
          activity.recreate();
        }
        return true;
      }
    });

    image_quality_control = (CheckBoxPreference) findPreference("setting_image_quality_control");
    image_quality_control.setChecked(Settings.getInstance().isLoadOriginalImage());
    image_quality_control.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bLoadOriginalImage = Settings.getInstance().isLoadOriginalImage();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bLoadOriginalImage = boolVal;
        }
        Settings.getInstance().setLoadOriginalImage(bLoadOriginalImage);
        return true;
      }
    });

    daynight_control = (CheckBoxPreference) findPreference("setting_daynight_control");
    daynight_control.setChecked(Settings.getInstance().isNightMode());
    daynight_control.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bNightMode = Settings.getInstance().isNightMode();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bNightMode = boolVal;
        }
        Settings.getInstance().setNightMode(bNightMode);

        setApplicationNightMode();
        return true;
      }
    });

    notification_control_mail = (CheckBoxPreference) findPreference("setting_notification_control_mail");
    notification_control_mail.setChecked(Settings.getInstance().isNotificationMail());
    notification_control_mail.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isNotificationMail();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setNotificationMail(bValue);
        return true;
      }
    });

    notification_control_at = (CheckBoxPreference) findPreference("setting_notification_control_at");
    notification_control_at.setChecked(Settings.getInstance().isNotificationAt());
    notification_control_at.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isNotificationAt();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setNotificationAt(bValue);
        return true;
      }
    });

    notification_control_like = (CheckBoxPreference) findPreference("setting_notification_control_like");
    notification_control_like.setChecked(Settings.getInstance().isNotificationLike());
    notification_control_like.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isNotificationLike();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setNotificationLike(bValue);
        return true;
      }
    });

    notification_control_reply = (CheckBoxPreference) findPreference("setting_notification_control_reply");
    notification_control_reply.setChecked(Settings.getInstance().isNotificationReply());
    notification_control_reply.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isNotificationReply();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setNotificationReply(bValue);
        return true;
      }
    });

    signature_control = (CheckBoxPreference) findPreference("setting_signature_control");
    signature_control.setChecked(Settings.getInstance().bUseSignature());
    signature_control.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().bUseSignature();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setUseSignature(bValue);
        if (bValue == false) {
          String alipay = "vinneyguo@outlook.com";
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final android.content.ClipboardManager clipboardManager =
                (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            final android.content.ClipData clipData = android.content.ClipData.newPlainText("ID", alipay);
            clipboardManager.setPrimaryClip(clipData);
          } else {
            final android.text.ClipboardManager clipboardManager =
                (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(alipay);
          }
          Toast.makeText(getActivity(), "???????????????ID?????????????????????...", Toast.LENGTH_SHORT).show();
        }
        return true;
      }
    });

    signature_content = findPreference("setting_signature_content");
    signature_content.setSummary(Settings.getInstance().getSignature());
    if (signature_content instanceof EditTextPreference) {
      // set default value in editing dialog
      EditTextPreference et = (EditTextPreference) signature_content;
      et.setText(Settings.getInstance().getSignature());
    }
    signature_content.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        String signature = newValue.toString();
        Settings.getInstance().setSignature(signature);
        signature_content.setSummary(signature);
        return true;
      }
    });

    topic_fwd_self = (CheckBoxPreference) findPreference("setting_topic_fwd");
    topic_fwd_self.setChecked(Settings.getInstance().isTopicFwdSelf());
    topic_fwd_self.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isTopicFwdSelf();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().SetTopicFwdSelf(bValue);
        return true;
      }
    });

    set_id_check = (CheckBoxPreference) findPreference("set_id_check");
    set_id_check.setChecked(Settings.getInstance().isSetIdCheck());
    set_id_check.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isSetIdCheck();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().SetIdCheck(bValue);
        return true;
      }
    });

    set_left_nav_slide = (CheckBoxPreference) findPreference("left_nav_slide");
    set_left_nav_slide.setChecked(Settings.getInstance().isLeftNavSlide());
    set_left_nav_slide.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean bValue = Settings.getInstance().isLeftNavSlide();
        if (newValue instanceof Boolean) {
          Boolean boolVal = (Boolean) newValue;
          bValue = boolVal;
        }
        Settings.getInstance().setLeftNavSlide(bValue);
        Activity activity = getActivity();
        if (activity != null) {
          Intent intent = new Intent(activity.getApplicationContext(), MainActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(intent);
          activity.finish();
        }
        return true;
      }
    });

    app_feedback = findPreference("app_feedback");
    app_feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {
        ComposePostContext postContext = new ComposePostContext();
        postContext.setComposingMode(ComposePostContext.MODE_NEW_MAIL_TO_USER);
        postContext.setPostAuthor("VINNEY");
        Intent intent = new Intent(getActivity(), ComposePostActivity.class);
        intent.putExtra(SMTHApplication.COMPOSE_POST_CONTEXT, postContext);
        startActivity(intent);
        return true;
      }
    });

    app_version = findPreference("setting_app_version");
    app_version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference preference) {

       // ActivityUtils.openLink("http://zsmth-android.zfdang.com/release.html", getActivity());
        Toast.makeText(SMTHApplication.getAppContext(),"??????Vinney???zSMTH????????????!",Toast.LENGTH_LONG).show();
        ActivityUtils.openLink("https://wws.lanzous.com/b01noyh6b", getActivity());
        return true;
      }
    });

    updateOkHttp3Cache();
    updateFrescoCache();
    updateVersionInfo();
  }

  @Override public void onCreatePreferences(Bundle bundle, String s) {
  }

  public void setApplicationNightMode() {
    boolean bNightMode = Settings.getInstance().isNightMode();
    if (bNightMode) {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    } else {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    Activity activity = getActivity();
    if (activity != null) {
      Intent intent = new Intent(activity.getApplicationContext(), MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      activity.finish();
    }
  }

  public void updateVersionInfo() {
    Context context = SMTHApplication.getAppContext();
    try {
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      String version = pInfo.versionName;
      int verCode = pInfo.versionCode;
      String summary = String.format("?????????: %s(%d)", version, verCode);
      app_version.setSummary(summary);
    } catch (Exception e) {
      Log.e(TAG, "updateVersionInfo: " + Log.getStackTraceString(e));
    }
  }

  public void updateOkHttp3Cache() {
    File httpCacheDirectory = new File(SMTHApplication.getAppContext().getCacheDir(), "Responses");
    updateCacheSize(httpCacheDirectory.getAbsolutePath(), okhttp3_cache);
  }

  public void updateFrescoCache() {
    File frescoCacheDirectory = new File(SMTHApplication.getAppContext().getCacheDir(), "image_cache");
    // Log.d(TAG, "updateFrescoCache: " + frescoCacheDirectory.getAbsolutePath());
    updateCacheSize(frescoCacheDirectory.getAbsolutePath(), fresco_cache);
  }

  public void updateCacheSize(final String folder, final Preference pref) {
    Observable.just(folder).map(new Function<String, String>() {
      @Override public String apply(@NonNull String s) throws Exception {
        return FileSizeUtil.getAutoFileOrFolderSize(s);
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
      @Override public void onSubscribe(@NonNull Disposable disposable) {

      }

      @Override public void onNext(@NonNull String s) {
        Log.d(TAG, "onNext: Folder size = " + s);
        pref.setSummary("????????????:" + s);
      }

      @Override public void onError(@NonNull Throwable e) {
        Toast.makeText(SMTHApplication.getAppContext(), "????????????????????????!\n" + e.toString(), Toast.LENGTH_LONG).show();
      }

      @Override public void onComplete() {
      }
    });
  }
}
