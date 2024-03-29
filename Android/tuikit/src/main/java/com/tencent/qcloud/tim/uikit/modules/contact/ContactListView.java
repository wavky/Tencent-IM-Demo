package com.tencent.qcloud.tim.uikit.modules.contact;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tencent.imsdk.TIMFriendshipManager;
import com.tencent.imsdk.TIMValueCallBack;
import com.tencent.imsdk.ext.group.TIMGroupBaseInfo;
import com.tencent.imsdk.ext.group.TIMGroupManagerExt;
import com.tencent.imsdk.friendship.TIMFriend;
import com.tencent.qcloud.tim.uikit.R;
import com.tencent.qcloud.tim.uikit.component.CustomLinearLayoutManager;
import com.tencent.qcloud.tim.uikit.component.indexlib.IndexBar.widget.IndexBar;
import com.tencent.qcloud.tim.uikit.component.indexlib.suspension.SuspensionDecoration;
import com.tencent.qcloud.tim.uikit.utils.TUIKitLog;
import com.tencent.qcloud.tim.uikit.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;


public class ContactListView extends LinearLayout {

    private static final String TAG = ContactListView.class.getSimpleName();

    private static final String INDEX_STRING_TOP = "↑";
    private RecyclerView mRv;
    private ContactAdapter mAdapter;
    private CustomLinearLayoutManager mManager;
    private List<ContactItemBean> mData = new ArrayList<>();
    private SuspensionDecoration mDecoration;
    private TextView mContactCountTv;

    /**
     * 右侧边栏导航区域
     */
    private IndexBar mIndexBar;

    /**
     * 显示指示器DialogText
     */
    private TextView mTvSideBarHint;

    public ContactListView(Context context) {
        super(context);
        init();
    }

    public ContactListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContactListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.contact_list, this);
        mRv = findViewById(R.id.contact_member_list);
        mManager = new CustomLinearLayoutManager(getContext());
        mRv.setLayoutManager(mManager);

        mAdapter = new ContactAdapter(mData);
        mRv.setAdapter(mAdapter);
        mRv.addItemDecoration(mDecoration = new SuspensionDecoration(getContext(), mData));
        //如果add两个，那么按照先后顺序，依次渲染。
        //使用indexBar
        mTvSideBarHint = findViewById(R.id.contact_tvSideBarHint);//HintTextView
        mIndexBar = findViewById(R.id.contact_indexBar);//IndexBar
        //indexbar初始化
        mIndexBar.setPressedShowTextView(mTvSideBarHint)//设置HintTextView
                .setNeedRealIndex(false)
                .setLayoutManager(mManager);//设置RecyclerView的LayoutManager
        mContactCountTv = findViewById(R.id.contact_count);
        mContactCountTv.setText(String.format(getResources().getString(R.string.contact_count), 0));
    }

    public ContactAdapter getAdapter() {
        return mAdapter;
    }

    public void setDataSource(List<ContactItemBean> data) {
        this.mData = data;
        mAdapter.setDataSource(mData);
        mIndexBar.setSourceDatas(mData).invalidate();
        mDecoration.setDatas(mData);
        mContactCountTv.setText(String.format(getResources().getString(R.string.contact_count), mData.size()));
    }

    public void setSingleSelectMode(boolean mode) {
        mAdapter.setSingleSelectMode(mode);
    }

    public void setOnSelectChangeListener(OnSelectChangedListener selectChangeListener) {
        mAdapter.setOnSelectChangedListener(selectChangeListener);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mAdapter.setOnItemClickListener(listener);
    }

    public interface OnSelectChangedListener {
        void onSelectChanged(ContactItemBean contact, boolean selected);
    }

    public interface OnItemClickListener {
        void onItemClick(int position, ContactItemBean contact);
    }

    public static class DataSource {
        public static final int UNKNOWN = -1;
        public static final int FRIEND_LIST = 1;
        public static final int BLACK_LIST = 2;
        public static final int GROUP_LIST = 3;
        public static final int CONTACT_LIST = 4;
    }

    public void loadDataSource(int dataSource) {
        switch (dataSource) {
            case DataSource.FRIEND_LIST:
                loadFriendListData(false);
                break;
            case DataSource.BLACK_LIST:
                loadBlackListData();
                break;
            case DataSource.GROUP_LIST:
                loadGroupListData();
                break;
            case DataSource.CONTACT_LIST:
                loadFriendListData(true);
                break;
        }
    }

    private void loadFriendListData(final boolean loopMore) {
        TIMFriendshipManager.getInstance().getFriendList(new TIMValueCallBack<List<TIMFriend>>() {
            @Override
            public void onError(int code, String desc) {
                TUIKitLog.e(TAG, "getFriendList err code = " + code);
            }

            @Override
            public void onSuccess(List<TIMFriend> timFriends) {
                TUIKitLog.i(TAG, "getFriendList success result = " + timFriends.size());
                mData.clear();
                if (loopMore) {
                    mData.add((ContactItemBean) new ContactItemBean(getResources().getString(R.string.new_friend))
                            .setTop(true).setBaseIndexTag(ContactItemBean.INDEX_STRING_TOP));
                    mData.add((ContactItemBean) new ContactItemBean(getResources().getString(R.string.group)).
                            setTop(true).setBaseIndexTag(ContactItemBean.INDEX_STRING_TOP));
                    mData.add((ContactItemBean) new ContactItemBean(getResources().getString(R.string.blacklist)).
                            setTop(true).setBaseIndexTag(ContactItemBean.INDEX_STRING_TOP));
                }
                for (TIMFriend timFriend : timFriends){
                    ContactItemBean info = new ContactItemBean();
                    info.covertTIMFriend(timFriend);
                    mData.add(info);
                }
                setDataSource(mData);
            }
        });
    }

    private void loadBlackListData() {
        TIMFriendshipManager.getInstance().getBlackList(new TIMValueCallBack<List<TIMFriend>>() {
            @Override
            public void onError(int i, String s) {
                TUIKitLog.e(TAG, "getBlackList err code = " + i + ", desc = " + s);
                ToastUtil.toastShortMessage("Error code = " + i + ", desc = " + s);
            }

            @Override
            public void onSuccess(List<TIMFriend> timFriends) {
                if (timFriends.size() == 0) {
                    ToastUtil.toastShortMessage("getBlackList success, no ids");
                    return;
                }
                mData.clear();
                for (TIMFriend timFriend : timFriends){
                    ContactItemBean info = new ContactItemBean();
                    info.covertTIMFriend(timFriend).setBlackList(true);
                    mData.add(info);
                }
                setDataSource(mData);
            }
        });
    }

    private void loadGroupListData() {
        TIMGroupManagerExt.getInstance().getGroupList(new TIMValueCallBack<List<TIMGroupBaseInfo>>() {

            @Override
            public void onError(int i, String s) {
                TUIKitLog.e(TAG, "getGroupList err code = " + i + ", desc = " + s);
                ToastUtil.toastShortMessage("Error code = " + i + ", desc = " + s);
            }

            @Override
            public void onSuccess(List<TIMGroupBaseInfo> infos) {
                TUIKitLog.i(TAG, "getFriendGroups success");
                mData.clear();
                for (TIMGroupBaseInfo info : infos) {
                    if (!TextUtils.equals(info.getGroupType(), "Private")) {
                        ContactItemBean bean = new ContactItemBean();
                        mData.add(bean.covertTIMGroupBaseInfo(info));
                    }
                }
                setDataSource(mData);
            }
        });
    }

    public List<ContactItemBean> getGroupData() {
        return mData;
    }
}
