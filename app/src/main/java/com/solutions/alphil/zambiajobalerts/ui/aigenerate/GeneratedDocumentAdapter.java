package com.solutions.alphil.zambiajobalerts.ui.aigenerate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.nativead.AdChoicesView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.solutions.alphil.zambiajobalerts.R;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocument;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GeneratedDocumentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final class BannerItem {
    }

    public interface OnDocumentActionListener {
        void onOpen(GeneratedDocument document);
        void onDelete(GeneratedDocument document);
    }

    private static final int VIEW_TYPE_NATIVE_AD = 0;
    private static final int VIEW_TYPE_BANNER = 1;
    private static final int VIEW_TYPE_DOCUMENT = 2;

    private List<Object> items = new ArrayList<>();
    private final OnDocumentActionListener listener;

    public GeneratedDocumentAdapter(OnDocumentActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_NATIVE_AD) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_native_ad, parent, false);
            return new NativeAdViewHolder(view);
        }
        if (viewType == VIEW_TYPE_BANNER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_banner_ad, parent, false);
            return new BannerAdViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_generated_document, parent, false);
        return new DocumentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof NativeAdViewHolder) {
            ((NativeAdViewHolder) holder).bind((NativeAd) item);
        } else if (holder instanceof BannerAdViewHolder) {
            ((BannerAdViewHolder) holder).bind();
        } else if (holder instanceof DocumentViewHolder) {
            bindDocument((DocumentViewHolder) holder, (GeneratedDocument) item);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof NativeAd) {
            return VIEW_TYPE_NATIVE_AD;
        }
        if (item instanceof BannerItem) {
            return VIEW_TYPE_BANNER;
        }
        return VIEW_TYPE_DOCUMENT;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void bindDocument(@NonNull DocumentViewHolder holder, GeneratedDocument item) {
        holder.tvTitle.setText(item.getFileName());
        holder.tvType.setText(item.getDisplayLabel());
        holder.tvSource.setText(item.getDisplaySource());
        holder.tvDate.setText(DateFormat.getDateTimeInstance().format(new Date(item.getCreatedAt())));

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpen(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
            }
        });
    }

    public void submitList(List<Object> newItems) {
        items = newItems == null ? new ArrayList<>() : newItems;
        notifyDataSetChanged();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof NativeAdViewHolder) {
            ((NativeAdViewHolder) holder).clear();
        }
    }

    static class NativeAdViewHolder extends RecyclerView.ViewHolder {
        NativeAdView nativeAdView;
        ImageView adAppIcon;
        TextView adHeadline, adBody, adAdvertiser, adAttribution;
        Button adCallToAction;
        AdChoicesView adChoicesView;

        NativeAdViewHolder(@NonNull View itemView) {
            super(itemView);
            nativeAdView = itemView.findViewById(R.id.native_ad_view);
            adAppIcon = itemView.findViewById(R.id.ad_app_icon);
            adHeadline = itemView.findViewById(R.id.ad_headline);
            adBody = itemView.findViewById(R.id.ad_body);
            adAdvertiser = itemView.findViewById(R.id.ad_advertiser);
            adCallToAction = itemView.findViewById(R.id.ad_call_to_action);
            adAttribution = itemView.findViewById(R.id.ad_attribution);
            adChoicesView = itemView.findViewById(R.id.ad_choices_view);
        }

        void bind(NativeAd nativeAd) {
            String headline = nativeAd.getHeadline();
            if (headline != null) {
                adHeadline.setText(headline);
                adHeadline.setVisibility(View.VISIBLE);
            } else {
                adHeadline.setVisibility(View.INVISIBLE);
            }

            String body = nativeAd.getBody();
            if (body != null) {
                adBody.setText(body);
                adBody.setVisibility(View.VISIBLE);
            } else {
                adBody.setVisibility(View.INVISIBLE);
            }

            String callToAction = nativeAd.getCallToAction();
            if (callToAction != null) {
                adCallToAction.setText(callToAction);
                adCallToAction.setVisibility(View.VISIBLE);
            } else {
                adCallToAction.setVisibility(View.INVISIBLE);
            }

            if (nativeAd.getIcon() != null) {
                adAppIcon.setImageDrawable(nativeAd.getIcon().getDrawable());
                adAppIcon.setVisibility(View.VISIBLE);
            } else {
                adAppIcon.setVisibility(View.INVISIBLE);
            }

            String advertiser = nativeAd.getAdvertiser();
            if (advertiser != null) {
                adAdvertiser.setText(advertiser);
                adAdvertiser.setVisibility(View.VISIBLE);
            } else {
                adAdvertiser.setVisibility(View.INVISIBLE);
            }

            if (adAttribution != null) {
                adAttribution.setVisibility(View.VISIBLE);
            }

            if (adChoicesView != null) {
                nativeAdView.setAdChoicesView(adChoicesView);
            }

            nativeAdView.setHeadlineView(adHeadline);
            nativeAdView.setBodyView(adBody);
            nativeAdView.setCallToActionView(adCallToAction);
            nativeAdView.setIconView(adAppIcon);
            nativeAdView.setAdvertiserView(adAdvertiser);
            nativeAdView.setNativeAd(nativeAd);
        }

        void clear() {
            adHeadline.setText("");
            adBody.setText("");
            adAdvertiser.setText("");
            adCallToAction.setText("");
            adAppIcon.setImageDrawable(null);
        }
    }

    static class BannerAdViewHolder extends RecyclerView.ViewHolder {
        AdView adView;

        BannerAdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = itemView.findViewById(R.id.itemBannerAdView);
        }

        void bind() {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    static class DocumentViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvType;
        TextView tvSource;
        TextView tvDate;
        Button btnOpen;
        Button btnDelete;

        DocumentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFileName);
            tvType = itemView.findViewById(R.id.tvDocumentType);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnOpen = itemView.findViewById(R.id.btnOpen);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
