package com.bhash.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bhash.R;
import com.bhash.presenter.activities.util.BRActivity;
import com.bhash.presenter.customviews.BRText;
import com.bhash.tools.animation.BRAnimator;
import com.bhash.tools.manager.BRSharedPrefs;
import com.bhash.tools.manager.FontManager;
import com.bhash.tools.security.AuthManager;
import com.bhash.tools.security.BRKeyStore;
import com.bhash.tools.util.BRConstants;
import com.bhash.tools.util.CurrencyUtils;
import com.bhash.wallet.WalletsMaster;
import com.bhash.wallet.abstracts.BaseWalletManager;
import com.bhash.wallet.wallets.bitcoin.WalletBitcoinManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


import static com.bhash.tools.util.BRConstants.ONE_BITCOIN;


public class SpendLimitActivity extends BRActivity {
    private static final String TAG = SpendLimitActivity.class.getName();
    public static boolean appVisible = false;
    private static SpendLimitActivity app;
    private ListView listView;
    private LimitAdaptor adapter;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    public static SpendLimitActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spend_limit);

        ImageButton faq = findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.fingerprintSpendingLimit);
            }
        });

        listView = findViewById(R.id.limit_list);
        listView.setFooterDividersEnabled(true);
        adapter = new LimitAdaptor(this);
        List<Integer> items = new ArrayList<>();
        items.add(getAmountByStep(0).intValue());
        items.add(getAmountByStep(1).intValue());
        items.add(getAmountByStep(2).intValue());
        items.add(getAmountByStep(3).intValue());
        items.add(getAmountByStep(4).intValue());

        adapter.addAll(items);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                int limit = adapter.getItem(position);
                Log.e(TAG, "limit chosen: " + limit);
                BRKeyStore.putSpendLimit(limit, app);
                long totalSent = 0;
                List<BaseWalletManager> wallets = WalletsMaster.getInstance(app).getAllWallets();
                for (BaseWalletManager w : wallets)
                    totalSent += w.getTotalSent(app); //collect total total sent
                AuthManager.getInstance().setTotalLimit(app, totalSent + BRKeyStore.getSpendLimit(app));
                adapter.notifyDataSetChanged();
            }

        });
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    //satoshis
    private BigDecimal getAmountByStep(int step) {
        BigDecimal result;
        switch (step) {
            case 0:
                result = new BigDecimal(0);// 0 always require
                break;
            case 1:
                result = new BigDecimal(ONE_BITCOIN / 100);//   0.01 BTC
                break;
            case 2:
                result = new BigDecimal(ONE_BITCOIN / 10);//   0.1 BTC
                break;
            case 3:
                result = new BigDecimal(ONE_BITCOIN);//   1 BTC
                break;
            case 4:
                result = new BigDecimal(ONE_BITCOIN * 10);//   10 BTC
                break;

            default:
                result = new BigDecimal(ONE_BITCOIN);//   1 BTC Default
                break;
        }
        return result;
    }

    private int getStepFromLimit(long limit) {
        switch ((int) limit) {

            case 0:
                return 0;
            case ONE_BITCOIN / 100:
                return 1;
            case ONE_BITCOIN / 10:
                return 2;
            case ONE_BITCOIN:
                return 3;
            case ONE_BITCOIN * 10:
                return 4;
            default:
                return 2; //1 BTC Default
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;

    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    public class LimitAdaptor extends ArrayAdapter<Integer> {

        private final Context mContext;
        private final int layoutResourceId;
        private BRText textViewItem;

        public LimitAdaptor(Context mContext) {

            super(mContext, R.layout.currency_list_item);

            this.layoutResourceId = R.layout.currency_list_item;
            this.mContext = mContext;
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            final long limit = BRKeyStore.getSpendLimit(app);
            if (convertView == null) {
                // inflate the layout
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }
            // get the TextView and then set the text (item name) and tag (item ID) values
            textViewItem = convertView.findViewById(R.id.currency_item_text);
            Integer item = getItem(position);
            BaseWalletManager walletManager = WalletBitcoinManager.getInstance(app); //use the bitcoin wallet to show the limits

            String cryptoAmount = CurrencyUtils.getFormattedAmount(app, walletManager.getIso(app), new BigDecimal(item));

            String text = String.format(item == 0 ? app.getString(R.string.TouchIdSpendingLimit) : "%s", cryptoAmount);
            textViewItem.setText(text);
            ImageView checkMark = convertView.findViewById(R.id.currency_checkmark);

            if (position == getStepFromLimit(limit)) {
                checkMark.setVisibility(View.VISIBLE);
            } else {
                checkMark.setVisibility(View.GONE);
            }
            return convertView;

        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public int getItemViewType(int position) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

    }

}
