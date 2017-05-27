package com.snailgame.mobilesdk;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.snail.pay.sdk.SnailPay;
import com.snail.pay.sdk.listener.PaymentListener;
import com.snail.pay.sdk.listener.PaymentListener.OnFinishListener;
import com.snail.statistics.SnailStatistics;
import com.snailgame.mobilesdk.aas.ui.DialogSnailActivation;
import com.snailgame.mobilesdk.aas.ui.SnailMainActivity;
import com.snailgame.mobilesdk.antiaddiction.AntiAddictionLogic;
import com.snailgame.mobilesdk.cardpay.SnailGameCardPay;
import com.snailgame.mobilesdk.cardpay.SnailGameCardPayFragment;
import com.snailgame.mobilesdk.entry.SnailAppInfo;
import com.snailgame.mobilesdk.entry.SnailBuyInfo;
import com.snailgame.mobilesdk.getui.GeTui;
import com.snailgame.mobilesdk.getui.UploadData;
import com.snailgame.mobilesdk.giftpacks.GiftPacksUtil;
import com.snailgame.mobilesdk.logout.LogoutDialog;
import com.snailgame.mobilesdk.open.OnAntiAddictionListener;
import com.snailgame.mobilesdk.open.OnCloseListener;
import com.snailgame.mobilesdk.open.OnRealNameListener;
import com.snailgame.mobilesdk.open.OnSnailGameCardPayListener;
import com.snailgame.mobilesdk.process.ProcessUtil;
import com.snailgame.mobilesdk.realname.RealNameLogic;
import com.snailgame.mobilesdk.update.UpdateService;
import com.snailgame.mobilesdk.upload.UpLoadConst;
import com.snailgame.mobilesdk.upload.UpLoadRequest;
import com.snailgame.mobilesdk.util.GameConst;
import com.snailgame.mobilesdk.util.GameSDKServerUtil;
import com.snailgame.mobilesdk.views.FloatView;
import com.snailgame.mobilesdk.views.MarqeenFloatManager;
import com.snailgame.sdkcore.aas.logic.LoginCallbackListener;
import com.snailgame.sdkcore.aas.logic.SdkServerUtil;
import com.snailgame.sdkcore.aas.logic.SnailLog;
import com.snailgame.sdkcore.aas.model.LoginRole;
import com.snailgame.sdkcore.bind.Bind;
import com.snailgame.sdkcore.entry.SnailBuy;
import com.snailgame.sdkcore.entry.SnailData;
import com.snailgame.sdkcore.init.InitRequset;
import com.snailgame.sdkcore.localdata.manifest.ManifestReader;
import com.snailgame.sdkcore.localdata.sharedprefs.ShareUtil;
import com.snailgame.sdkcore.localdata.sharedprefs.SharedReader;
import com.snailgame.sdkcore.localdata.sharedprefs.SharedWriter;
import com.snailgame.sdkcore.model.SnailOrdinaryListener;
import com.snailgame.sdkcore.open.BindCallbackListener;
import com.snailgame.sdkcore.open.ErrorCode;
import com.snailgame.sdkcore.open.MiscCallbackListener;
import com.snailgame.sdkcore.open.OnActiveResultListener;
import com.snailgame.sdkcore.open.RoleIdentityListener;
import com.snailgame.sdkcore.open.SDKType;
import com.snailgame.sdkcore.register.SnailRegister;
import com.snailgame.sdkcore.util.Const;
import com.snailgame.sdkcore.util.LogUtil;
import com.snailgame.sdkcore.util.SnailUtil;
import com.snaillogin.SnailLoginManager;
import com.snaillogin.SnailRegisterManager;
import com.snaillogin.SnailSDK;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnailCommplatform {

	private static final String TAG = "SnailCommplatform";

	public static final int SCREEN_ORIENTATION_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

	public static final int SCREEN_ORIENTATION_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;

	public static final int SCREEN_ORIENTATION_SENSOR_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

	public Map<Context, FloatView> floatviews;

	private Context mContext;
	private SnailAppInfo mSnailAppInfo;
	private Handler handler;
	private Dialog dialog;

	private static SnailCommplatform instance;

	private SnailCommplatform() {
		floatviews = new HashMap<>();
		handler = new Handler(Looper.getMainLooper());
	}

	public static synchronized SnailCommplatform getInstance() {
		if (instance == null) {
			instance = new SnailCommplatform();
		}
		return instance;
	}

	public Context getContext() {
		return mContext;
	}

	/**
	 * 初始化SDK
	 *
	 * @param activity
	 *            游戏Activity
	 * @param snailAppInfo
	 *            封装appId与appKey
	 * @param onInitCompleteListener
	 *            回调
	 */
	public void snailInit(final Activity activity, SnailAppInfo snailAppInfo,
			final OnInitCompleteListener onInitCompleteListener) {
		activity.startService(new Intent(activity, UpdateService.class));
		// 判断是否可以新版本渠道包
		AssetManager aManager = activity.getAssets();
		String[] names;
		try {
			boolean isNewSDK = false;
			names = aManager.list("");
			for (String name : names) {
				if (name.startsWith("0snailsdk")) {
					LogUtil.d(TAG, "New SDK:" + name);
					isNewSDK = true;
					break;
				}
			}
			if (!isNewSDK) {
				showCrashDialog(activity);
				return;
			}
		} catch (IOException e) {
			LogUtil.w(TAG, "asstes list IOException:" + e.getMessage());
		}

		Const.curSDKType = SDKType.GAME;

		mContext = activity.getApplicationContext();

		SnailData.getInstance().setContext(mContext);

		// 显示闪屏Dialog
		handler.post(new Runnable() {

			@Override
			public void run() {
				if (dialog == null) {
					showDialog(activity);
					dialog.show();
				} else {
					if (dialog.getContext() == activity) {
						if (!dialog.isShowing()) {
							dialog.show();
						}
					} else {
						showDialog(activity);
					}
				}
			}
		});

		// 是否开启日志搜集
		try {
			ApplicationInfo appInfo = activity.getPackageManager()
					.getApplicationInfo(activity.getPackageName(),
							PackageManager.GET_META_DATA);
			if (appInfo.metaData != null) {
				SnailLog.needCollect = appInfo.metaData.getBoolean(
						"snail_collect", true);
				LogUtil.d(TAG, "SnailLog needCollect meta_data is: "
						+ SnailLog.needCollect);
			}
		} catch (Exception e) {
			LogUtil.w(TAG, "get meta-data for collect open failure!");
			LogUtil.w(TAG, e);
		}

		final long showTime = System.currentTimeMillis();
		LogUtil.d(TAG, "init current time is " + showTime);

		// 设置字体为默认大小
		Resources res = mContext.getResources();
		Configuration config = new Configuration();
		config.setToDefaults();
		res.updateConfiguration(config, res.getDisplayMetrics());

		// 获取商店aid开启免流量
		// startProxy(activity);

		mSnailAppInfo = snailAppInfo;
		SnailData snailData = SnailData.getInstance();
		snailData.setAppId(getAppId());
		snailData.setPlatformAppId(getAppId());
		snailData.setAppKey(getAppKey());
		snailData.setContext(mContext);

		// 查询开关配置并缓存
		new Thread() {
			public void run() {
				GameSDKServerUtil.QueryConfig();
			}
		}.start();

		SnailLog.usedStartTime = System.currentTimeMillis();
		SnailLog.switchStartTime = System.currentTimeMillis();
		SnailLog.dataCollectionInit(mContext, snailAppInfo.getAppId(), "", "",
				"", "", "8");
		SharedWriter sharedWriter = new SharedWriter();
		sharedWriter.saveChannelId("8");
		sharedWriter.saveLoginChannel("630");
		SnailStatistics.commitAllData(activity);
		SnailLog.collectActive(activity);
		SnailLog.collectInit(activity);

		//初始化礼包参数
		new Thread() {
			public void run() {
				GiftPacksUtil.getInstance().initData(getAppId());
			}
		}.start();

		// 判断app_key和app_id是否正确
		SdkServerUtil.INIT_STATE = 0;

		if (new SharedReader().getInit()) {
			if (new SharedReader().isLogin()) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						SdkServerUtil.verifyLogin();
					}
				}).start();
			} else {
				SdkServerUtil.INIT_STATE = 1;
			}
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					SdkServerUtil.verifyAppInfo();
				}
			}).start();
		}
		forCheckInit(activity, onInitCompleteListener);

		//获取倒计时配置时长
		new Thread() {
			public void run() {
				new InitRequset().getSmsTime("262a59BCe3cd47429E8B5Cb12B1d3daE6021f147B72c4929",
						"c33D59288cFf4A6DbCb57b361eb684eedbe71c902CA04820");
			}
		}.start();

		//初始化计费SDK
		SnailSDK.InitParams(activity, Const.Access.ACCESS_ID+"", Const.Access.ACCESS_PASSWD,
				Const.Access.ACCESS_TYPE+"", Const.Access.SEED);
		SnailSDK.addGameId("36");
		LogUtil.d("SnailCommplatform_init", new ManifestReader().getMeidaID());

		SnailSDK.addChannelId(new ManifestReader().getMeidaID());
		SnailSDK.addPid("8");//设置pid，运营商id,官方是7
		SnailSDK.addCommonRegisterType("SS");//设置  fromType（帐密注册时额外参数中的一值），默认“SJ"
		SnailSDK.addOneKeyRegisterType("MWA");//设置  fromType（上行短信一键注册时额外参数中的一值），默认“SWA"
//		SnailSDK.setLogOpen(true);
//		SnailSDK.setSandBox(true);
//		SnailRegisterManager.setLogOpen(true);
//		SnailRegisterManager.setSandBox(true);
//		SnailLoginManager.setLogOpen(true);
//		SnailLoginManager.setSandBox(true);
	}

	private void forCheckInit(final Activity activity,
			final OnInitCompleteListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(Const.INIT_TIME);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int count = 0;
				while (SdkServerUtil.INIT_STATE == 0) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					count++;
					if (count >= 30) {
						SdkServerUtil.INIT_STATE = -1;
					}
				}
				handler.post(new Runnable() {
					@Override
					public void run() {
						LogUtil.d(TAG, "init end code: "
								+ SdkServerUtil.INIT_STATE);
						cancelDialog();
						if (SdkServerUtil.INIT_STATE == 1) {
							new ProcessUtil(activity);
							if (listener != null) {
								listener.onComplete(OnInitCompleteListener.FLAG_NORMAL);
							}
						} else if (SdkServerUtil.INIT_STATE == -1) {
							if (listener != null) {
								listener.onComplete(OnInitCompleteListener.FLAG_FORCE_CLOSE);
							}
						}
					}
				});
			}
		}).start();
	}

	private void showDialog(Context context) {
		dialog = new Dialog(context, SnailUtil.getResId(context,
				Const.Res.DIALOG_FULLSCREEN, Const.Res.TYPE_STYLE));
		dialog.setCancelable(true);
		dialog.setContentView(SnailUtil.getLayoutId(context,
				Const.Res.INIT_DIALOG));
		dialog.show();
	}

	private void cancelDialog() {
		if (dialog != null)
			dialog.dismiss();
	}

	private void showCrashDialog(final Activity activity) {
		handler.post(new Runnable() {

			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				AlertDialog tempD = new AlertDialog.Builder(activity).create();
				tempD.setTitle("错误");
				tempD.setMessage("assets目录缺少SDK文件!");
				tempD.setCancelable(false);
				tempD.setButton("关闭", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						System.exit(0);
					}
				});
				tempD.show();
			}
		});
	}

	public int getAppId() {
		if (mSnailAppInfo != null) {
			return mSnailAppInfo.getAppId();
		}
		return -1;
	}

	public String getAppKey() {
		if (mSnailAppInfo != null) {
			return mSnailAppInfo.getAppKey();
		}
		return null;
	}

	public void snailLogin(final Activity activity,
			OnLoginProcessListener onLoginProcessListener) {
		mContext = activity;
		FloatView.isShow = true;
		SnailMiscCallbackListener.setOnLoginProcessListener(activity,
				onLoginProcessListener);
		if (!new SharedReader().getInit()) {
			SnailMiscCallbackListener.finishLoginProcess(activity,
					SnailErrorCode.SNAIL_COM_PLATFORM_ERROR_UNINIT);
			return;
		}
		SharedReader sharedReader = new SharedReader();
		sharedReader.checkChangPwdFlag();

		if (isLogined()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					final ProgressDialog proDialog = new ProgressDialog(
							activity);
					proDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					proDialog.setCancelable(true);
					proDialog.setTitle(Const.Value.ACCOUNT_VERIFY);
					proDialog.show();

					if (SnailData.getInstance().isActivationCodeOpen()) {
						SdkServerUtil
								.getActiveState(new OnActiveResultListener() {

									@Override
									public void needActive() {
										proDialog.dismiss();
										DialogSnailActivation dialog = new DialogSnailActivation(
												activity);
										dialog.show();
									}

									@Override
									public void unwantedActive() {
										verifyRole(activity, proDialog);
									}
								});
					} else {
						verifyRole(activity, proDialog);
					}

				}
			});
			return;
		}

		boolean hascache = false;
		if (new SharedReader().getIsOneKey(true)) {
			// 检查是否有缓存
			if (sharedReader.checkAid() && sharedReader.checkUuid()) {
				hascache = true;
			}
		} else {
			if (sharedReader.checkAccount() && sharedReader.checkPassword()) {
				hascache = true;
			}
		}
		if (!hascache) {
			Uri uri = Uri
					.parse("content://com.snailgame.mobilesdk.provider/get_login_data");
			Cursor cursor = null;
			try {
				cursor = activity.getContentResolver().query(uri, null, null,
						null, null);
				LogUtil.d(TAG, "get login data cursor is " + cursor);
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					boolean isOneKey = (cursor.getInt(cursor
							.getColumnIndex(Const.Key.IS_ONE_KEY)) == 1);
					ShareUtil.save(activity, Const.Key.IS_ONE_KEY, isOneKey);

					Bundle bundle = new Bundle();
					bundle.putBoolean(Const.Key.IS_ONE_KEY, isOneKey);

					if (isOneKey) {
						String aid = cursor.getString(cursor
								.getColumnIndex(Const.Key.AID));
						LogUtil.d(TAG, "aid is " + aid);
						String uuid = cursor.getString(cursor
								.getColumnIndex(Const.Key.UUID));
						LogUtil.d(TAG, "uuid is " + uuid);

						bundle.putString(Const.Key.AID, aid);
						bundle.putString(Const.Key.UUID, uuid);

					} else {
						String account = cursor.getString(cursor
								.getColumnIndex(Const.Key.ACCOUNT));
						String password = cursor.getString(cursor
								.getColumnIndex(Const.Key.PASSWORD));
						boolean isRandomReg = (cursor
								.getInt(cursor
										.getColumnIndex(Const.Key.IS_FROM_RANDOM_REG)) == 1);
						LogUtil.d(TAG, "account is " + account);
						LogUtil.d(TAG, "password is " + password);
						LogUtil.d(TAG, "isRandomReg is " + isRandomReg);

						bundle.putString(Const.Key.ACCOUNT, account);
						bundle.putString(Const.Key.PASSWORD, password);
						bundle.putBoolean(Const.Key.IS_FROM_RANDOM_REG,
								isRandomReg);
					}

					try {
						String alias = cursor.getString(cursor
								.getColumnIndex(Const.Key.ALIAS));
						LogUtil.d(TAG, "alias is " + alias);

						bundle.putString(Const.Key.ALIAS, alias);

					} catch (Exception e) {
						LogUtil.w(TAG, "can't find alias from store");
					}

					SnailMainActivity
							.actionStartForStoreLogin(activity, bundle);
					return;
				}
			} catch (Exception e) {
				LogUtil.w(TAG, e);
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
		SnailMainActivity.actionStartForLogin(activity);
	}

	private void verifyRole(final Activity activity,
			final ProgressDialog proDialog) {
		SdkServerUtil.getRoleData(activity, new LoginCallbackListener() {
			@Override
			public void forwardTo() {
				HashMap<String, String>[] mapTeam = LoginRole.getInstance()
						.getRoleMap();
				if (mapTeam == null || mapTeam.length < 1) {
					proDialog.dismiss();
					SdkServerUtil.gameLoginDefault(activity);
				} else if (mapTeam.length == 1) {
					SdkServerUtil.getRoleIdentity(
							mapTeam[0].get("nRoleUserId"),
							new RoleIdentityListener() {
								@Override
								public void result(int code, String[] data) {
									proDialog.dismiss();
									SdkServerUtil.gameLoginByTeam(code, data,
											activity);
								}
							});
				} else {
					proDialog.dismiss();
					SnailMainActivity.actionStartForChooseRole(activity);
				}
			}

			@Override
			public void end(int endState, int errorCode) {
				proDialog.dismiss();
				SnailMiscCallbackListener.finishLoginProcess(activity,
						errorCode);
			}
		});
	}

	/**
	 * 调用清除数据并回调
	 */
	private void snailLogoutCallBack(Context context, Boolean isPopup) {
		snailLogout(context);
		if (!isPopup) {
			((Activity) context).finish();
		}
		if (logoutListener != null) {
			logoutListener.result();
		}
	}

	/**
	 * 登出清除数据 新版本提供登出回调功能，如果设置了登出回调，则此方法会在登出时内部调用
	 */
	public void snailLogout(Context context) {
		if (mSnailAppInfo != null && mSnailAppInfo.getAppId() > 0
				&& isLogined()) {
			SnailLog.collectOut(mContext, getAppId(), getDefSessionId(),
					getDefLoginUin());
			long currentTime = System.currentTimeMillis();
			if (SnailLog.usedStartTime > 0
					&& SnailLog.usedStartTime < currentTime) {
				long usedTime = (currentTime - SnailLog.usedStartTime) / 1000;
				SnailLog.collectLogout(context, usedTime);
			}
		}
		if (context instanceof Activity) {
			showFloatView(false);
		}

		SdkServerUtil.logout();

		// 删除部分单例缓存
		LoginRole.getInstance().clear();
	}

	public boolean isLogined() {
		return mContext != null && SnailData.getInstance().isLogin();
	}

	private String getDefLoginUin() {
		return new SharedReader().getUid();
	}

	private String getDefSessionId() {
		return new SharedReader().getSessionId();
	}

	public String getLoginUin() {
		return new SharedReader().getRoleUserId();
	}

	public String getSessionId() {
		return new SharedReader().getRoleSession();
	}

	public int snailUniPay(SnailBuyInfo buyInfo, Activity activity,
			OnPayProcessListener onPayProcessListener) {
		if (onPayProcessListener == null || buyInfo == null) {
			return SnailErrorCode.SNAIL_COM_PLATFORM_ERROR_PARAM_IS_NULL;
		}
		if (!isLogined()) {
			return SnailErrorCode.SNAIL_COM_PLATFORM_ERROR_NEED_LOGIN;
		}
		snailUniPay(buyInfo, activity, onPayProcessListener, false);
		return 0;
	}

	private void snailUniPay(SnailBuyInfo buyInfo, Activity activity,
			OnPayProcessListener onPayProcessListener, boolean forceUseTTCoin) {
		SnailMiscCallbackListener.setOnPayProcessListener(onPayProcessListener);
		SnailBuy snailBuy = getSnailBuy(buyInfo);
		GameSDKServerUtil.getTicketList(activity, snailBuy, forceUseTTCoin,
				true);
	}

	public int snailUniPayAsyn(SnailBuyInfo buyInfo, Activity activity,
			OnPayProcessListener onPayProcessListener) {
		if (onPayProcessListener == null || buyInfo == null) {
			return SnailErrorCode.SNAIL_COM_PLATFORM_ERROR_PARAM_IS_NULL;
		}
		if (!isLogined()) {
			return SnailErrorCode.SNAIL_COM_PLATFORM_ERROR_NEED_LOGIN;
		}
		snailUniPayAsyn(buyInfo, activity, onPayProcessListener, false);
		return 0;
	}

	private void snailUniPayAsyn(SnailBuyInfo buyInfo, Activity activity,
								  OnPayProcessListener onPayProcessListener, boolean forceUseTTCoin) {
		SnailMiscCallbackListener.setOnPayProcessListener(onPayProcessListener);
		SnailBuy snailBuy = getSnailBuy(buyInfo);
		GameSDKServerUtil.getTicketList(activity, snailBuy, forceUseTTCoin,
				false);
	}

	public void snailSetScreenOrientation(int orientation) {
		SnailData.getInstance().setScreenOrientation(orientation);
	}

	public void snailOnResume() {
		if (mSnailAppInfo != null && mSnailAppInfo.getAppId() > 0
				&& mContext != null) {
			if (isLogined()) {
				SnailLog.collectEnter(mContext, getAppId(), getDefSessionId(),
						getDefLoginUin());
			}
			SnailLog.switchStartTime = System.currentTimeMillis();
			SnailLog.collectOnResume(mContext);
		}
	}

	public void snailOnPause() {
		if (mSnailAppInfo != null && mSnailAppInfo.getAppId() > 0) {
			if (isLogined()) {
				SnailLog.collectOut(mContext, getAppId(), getDefSessionId(),
						getDefLoginUin());
			}
			long currentTime = System.currentTimeMillis();
			if (SnailLog.switchStartTime > 0
					&& SnailLog.switchStartTime < currentTime) {
				long time = (currentTime - SnailLog.switchStartTime) / 1000;
				SnailLog.collectOnPause(mContext, time);
				SnailLog.switchStartTime = 0;
			}
		}
	}

	public void snailSetGameServerInfo(String serverId, String serverName) {
		UpLoadConst.SERVERID = serverId;
		UpLoadConst.SERVERNAME = serverName;
	}

	public void snailOnDestroy() {
		//关闭个推
		new GeTui().stopService();
		//关闭跑马灯(未开启也无影响)
		MarqeenFloatManager.getInstance().destory();
	}

	public void uploadOnLineTime(String time){
		new UpLoadRequest().snailOnlineTime(time);
	}

	private SnailBuy getSnailBuy(SnailBuyInfo buyInfo) {
		SnailBuy snailBuy = new SnailBuy();
		snailBuy.setCount(1);
		snailBuy.setPayDescription(buyInfo.getPayDescription());
		snailBuy.setProductId(buyInfo.getProductId());
		snailBuy.setProductName(buyInfo.getProductName());
		snailBuy.setProductPrice(buyInfo.getProductPrice());
		snailBuy.setSerial(buyInfo.getSerial());
		return snailBuy;
	}



	private void commitCallBack(OnCommitCallback call, int code) {
		if (call != null) {
			call.result(code);
		}
	}

	/**
	 * 上传玩家等级
	 */
	public void snailLevelUpload(int iGameLevel, String sRoleName, final OnCommitCallback callback) {
		new UpLoadRequest().snailLevelUpload(iGameLevel, sRoleName, callback);
	}

	public void snailNetworkHallPay(Activity activity, String account,
			String productName, String orderNum, String orderMoney,
			String orderSource, int platformId,
			OnPayProcessListener onPayProcessListener) {
		SnailMiscCallbackListener.setOnPayProcessListener(onPayProcessListener);
		SnailPay.gameToPayByConfirm(activity, account, productName, orderNum,
				orderMoney, orderSource, platformId, new OnFinishListener() {
					@Override
					public void finishPayProcess(int result) {
						LogUtil.d(TAG, "snailNetworkHallPay code is" + result);
						switch (result) {
						case PaymentListener.PAY_SUCCESS:
							MiscCallbackListener
									.finishPayProcess(ErrorCode.SNAIL_COM_PLATFORM_SUCCESS);
							break;
						case PaymentListener.PAY_FAILURE:
							MiscCallbackListener
									.finishPayProcess(ErrorCode.SNAIL_COM_PLATFORM_ERROR_PAY_FAILURE);
							break;
						default:
							MiscCallbackListener
									.finishPayProcess(ErrorCode.SNAIL_COM_PLATFORM_ERROR_PAY_FAILURE);
							break;
						}
					}
				});

	}

	/**
	 * 提交玩家已完成的任务
	 *
	 * @param iTaskId
	 *            任务ID
	 * @param sTaskName
	 *            任务名称
	 */
	public void snailSubmitTask(int iTaskId, String sTaskName, final OnCommitCallback callback) {
		new UpLoadRequest().snailSubmitTask(iTaskId, sTaskName, callback);
	}

	/**
	 * 提交游戏内玩家消费的金额
	 *
	 * @param iCost
	 *            单位：元（小数取整数部分）
	 */
	public void snailCost(int iCost, final OnCommitCallback callback) {
		new UpLoadRequest().snailCost(iCost, callback);
	}

	/**
	 * 提交玩家在游戏内的排名
	 *
	 * @param iRank
	 *            玩家的排名
	 */
	public void snailRank(int iRank, final OnCommitCallback callback) {
		new UpLoadRequest().snailRank(iRank, callback);
	}

	/**
	 * 好友数目
	 */
	public void snailFriendNum(int iFriends, final OnCommitCallback callback) {
		new UpLoadRequest().snailFriendNum(iFriends, callback);
	}

	public void createFloatView(Activity act, boolean show) {
		createFloatView(act, show, -1f);
	}

	public void createFloatView(final Activity act, final boolean show, float toTopPercent) {
		if (!isLogined())
			return;
		FloatView view = floatviews.get(act);
		if (view == null) {
			view = new FloatView(act, toTopPercent);
			floatviews.put(act, view);
		}
		GameSDKServerUtil.getAnnouncementNum(new SnailOrdinaryListener() {
			@Override
			public void result(int code) {
				SnailData.getInstance().setAnnouncementNum(code);
				showFloatView(show);
			}
		});
	}

	public void showFloatView(boolean show) {
		if (show && !isLogined())
			return;
		for (FloatView view : floatviews.values()) {
			if (view != null) {
				if (show) {
					view.refrushFloatView();
				}
				view.setVisibility(show ? View.VISIBLE : View.GONE);
			}
		}

	}

	public void destoryFloatView(Activity act) {
		FloatView view = floatviews.get(act);
		if (view != null) {
			view.destory();
			floatviews.remove(act);
		}
	}

	/**
	 * 是否已绑定过手机号
	 *
	 * @param callback
	 *            0表示有，1表示没有
	 */
	public void hasBindedPhone(final OnCommitCallback callback) {
		new Bind().getBindMobileNum(
				new BindCallbackListener() {

					@Override
					public void onResult(int code, String bindValue) {
						if (code == BindCallbackListener.BINDED) {
							// 有绑定
							commitCallBack(callback, 0);
						} else {
							commitCallBack(callback, 1);
						}
					}

					@Override
					public void onResult(int code) {
						commitCallBack(callback, 1);
					}
				});
	}

	/**
	 * 登录成功后显示帐号并提供帐号切换功能 目前没有帐号切换，只有注销并退出游戏
	 */
	public void changeAccountPopup(final Activity act) {
		LayoutInflater inflater = LayoutInflater.from(act);
		View view = inflater.inflate(
				SnailUtil.getLayoutId(act, Const.Res.CHANGE_ACCOUNT_POPUPWIN),
				null);

		final PopupWindow popup = new PopupWindow(view,
				WindowManager.LayoutParams.MATCH_PARENT, SnailUtil.dip2px(act,
						60));
		popup.setFocusable(true);
		ColorDrawable dw = new ColorDrawable(0xb0000000);
		popup.setBackgroundDrawable(dw);
		popup.setAnimationStyle(SnailUtil.getResId(act,
				Const.Res.ACCOUNT_POPUP_ANIM, Const.Res.TYPE_STYLE));
		popup.showAtLocation(view, Gravity.TOP, 0, 0);
		TextView accountTxt = (TextView) view.findViewById(SnailUtil.getResId(
				act, Const.Res.ACCOUNT_TEXTVIEW, Const.Res.TYPE_ID));
		accountTxt.setText(new SharedReader().getShowAlias());
		Button changeBtn = (Button) view.findViewById(SnailUtil.getResId(act,
				Const.Res.SNAIL_SUBMIT, Const.Res.TYPE_ID));
		changeBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				popup.dismiss();
				exitConfirm(act, true);
			}
		});
		view.postDelayed(new Runnable() {

			@Override
			public void run() {
				popup.dismiss();
			}
		}, 2500);
		popup.setOnDismissListener(new PopupWindow.OnDismissListener() {

			@Override
			public void onDismiss() {
				String aid = new SharedReader().getAid();
				//弹出广告框之前判断此用户如果是第一次在本手机上登录
				Boolean isFirst = ShareUtil.read(act, "gift_firstLogin"+ aid, true);
				//后台配置是否首次弹框
				Boolean configFirstPopup = SnailData.getInstance().isGiftpacksPopupByFirstLogin();
				//首次登录并且配置为首次不弹，则不弹框
				if(isFirst && !configFirstPopup){
					ShareUtil.save(act, "gift_firstLogin"+ aid, false);
				}else{
					GiftPacksUtil.getInstance().showGiftDialog(getContext());
				}
			}
		});
	}

	/**
	 * 设置游戏服务器ID（自研游戏兑换用）
	 */
	public void setGameServerId(String id) {
		SnailData.getInstance().setGameServerId(id);
	}

	/**
	 * 注销前退出游戏的确认提示
	 */
	public void exitConfirm(final Activity act, final Boolean isPopup) {
		new AlertDialog.Builder(act)
				.setTitle(Const.Value.IMPORTANT_TIP)
				.setMessage(
						logoutListenerIsNull() ? Const.Value.IMPORTANT_TIP_MESSAGE
								: GameConst.ConstStringCN.USER_CENTER_DIALOG_MSG)
				.setPositiveButton(Const.Value.IMPORTANT_SURE,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								if (logoutListenerIsNull()) {
									logout(act, isPopup);
								} else {
									snailLogoutCallBack(act, isPopup);
								}

							}
						})
				.setNegativeButton(Const.Value.IMPORTANT_CANCLE,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						}).create().show();
	}

	/**
	 * 退出游戏并消除数据
	 */
	private void logout(Activity act, Boolean isPopup) {
		@SuppressWarnings("deprecation")
		List<RunningTaskInfo> tasks = ((ActivityManager) act
				.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1);
		if (!tasks.isEmpty()) {
			ComponentName baseActivity = tasks.get(0).baseActivity;

			if (!baseActivity.getPackageName().equals(act.getPackageName())) {
				return;
			}
			SnailData snailData = SnailData.getInstance();
			SnailLog.collectOut(act, snailData.getAppId(),
					snailData.getSessionId(), snailData.getLoginUin());
			long currentTime = System.currentTimeMillis();
			if (SnailLog.usedStartTime > 0
					&& SnailLog.usedStartTime < currentTime) {
				long usedTime = (currentTime - SnailLog.usedStartTime) / 1000;
				SnailLog.collectLogout(act, usedTime);
			}
			SdkServerUtil.logout();
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// 注意
			intent.addCategory(Intent.CATEGORY_HOME);
			act.startActivity(intent);
			if (!isPopup) {
				act.finish();
			}
			for (Context cont : floatviews.keySet()) {
				if (cont instanceof Activity) {
					Activity act2 = (Activity) cont;
					if (!act2.isFinishing()) {
						act2.finish();
					}
				}
			}
			new Handler().postDelayed(new Runnable() {

				@Override
				public void run() {
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}, 1000);
		}
	}

	/**
	 * 提供注销回调的接口
	 */
	private OnLogoutListener logoutListener = null;

	public void snailSetLogoutListener(OnLogoutListener lisntener) {
		logoutListener = lisntener;
	}

	/**
	 * 判断登出回调是否为空，暂时用于注销Dialog的文字显示
	 *
	 */
	public boolean logoutListenerIsNull() {
		return logoutListener == null;
	}

	@SuppressWarnings("UnusedDeclaration")//游戏调用
	public void setAutoRealName(boolean isAuto) {
		SnailData.getInstance().setAutoRealName(isAuto);
	}

	@SuppressWarnings("UnusedDeclaration")//游戏调用
	/**
	 * 自研游戏调用
	 * 根据需要显示实名认证界面
	 */
	public void showRealName(Activity activity, boolean canClose, OnRealNameListener listener) {
		if(isLogined())  {
			RealNameLogic.showRealName(activity, canClose, listener);
		}else {
			if( null != listener) {
				listener.onFailure(0);
			}
		}
	}

	/**
	 * 使用蜗牛卡进行充值
	 */
	public void snailGameCardPay(Activity activity, String areaId, String roleId, OnSnailGameCardPayListener listener){
		SnailGameCardPayFragment.listener = listener;
		if (!isLogined()) {
			listener.payFaild(SnailErrorCode.SNAIL_COM_PLATFORM_ERROR_NEED_LOGIN);
			return;
		}
		SnailGameCardPay cardPay = new SnailGameCardPay();
		cardPay.setAreaId(areaId);
		cardPay.setRoleId(roleId);
		SnailMainActivity.actionStartForGameCardPay(activity, cardPay);
	}

	public void onStart() {
		UploadData.getInstance().startUpTime();
	}

	public void onStop() {
		UploadData.getInstance().stopUpTime();
	}

	/**
	 * 开启推送跑马灯以及配置服务器数据
     */
	public void snailSetPush(Activity activity, String serverId,String serverName) {
		snailSetGameServerInfo(serverId, serverName);
		MarqeenFloatManager.getInstance().initMarqeen(activity);
		new GeTui().setTag();
	}

	/**
	 * 跑马灯效果创建
	 */
	public void createMarqeen(Activity activity){
		MarqeenFloatManager.getInstance().initMarqeen(activity);
	}

	/**
	 * 退出游戏的时候调用
	 */
	public void onLogout(Activity activity, OnCloseListener listener) {
		LogoutDialog exitDialog = new LogoutDialog(activity, listener);
		exitDialog.show();
	}

	/**
	 * 自研游戏传入meidaId
	 */
	public void setGameMeidId(String meidId){
		SnailData.getInstance().setMeidaId(meidId);
	}

	/**
	 * 防沉迷接口(不需要登录也能使用)
	 */
	public void showAntiAddiction(Activity activity, String account, OnAntiAddictionListener listener){
		showAntiAddiction(activity, account, true, listener);
	}

	/**
	 * 防沉迷接口(不需要登录也能使用)
	 * @param canClose 是否能关闭
	 */
	public void showAntiAddiction(Activity activity, String account, boolean canClose, OnAntiAddictionListener listener){
		AntiAddictionLogic.showAntiAddiction(activity, account, canClose, listener);
	}

	/**
	 * 获取账号(明文账号)
	 */
	public String getAccount() {
		return new SharedReader().getPlainAccount();
	}

	//测试
	public String aa(){
		return "2222222222222222";
	}

}
