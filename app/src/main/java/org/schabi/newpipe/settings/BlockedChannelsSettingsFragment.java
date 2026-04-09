package org.schabi.newpipe.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.schabi.newpipe.R;
import org.schabi.newpipe.database.channel.model.BlockedChannelEntity;
import org.schabi.newpipe.databinding.FragmentBlockedChannelsBinding;
import org.schabi.newpipe.databinding.ItemBlockedChannelBinding;
import org.schabi.newpipe.local.channel.BlockedChannelManager;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class BlockedChannelsSettingsFragment extends Fragment {

    private FragmentBlockedChannelsBinding binding;
    private BlockedChannelManager blockedChannelManager;
    private BlockedChannelsAdapter adapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        blockedChannelManager = BlockedChannelManager.getInstance(requireContext());
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        binding = FragmentBlockedChannelsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view,
                              @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new BlockedChannelsAdapter();
        binding.blockedChannelsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.blockedChannelsList.setAdapter(adapter);

        disposables.add(blockedChannelManager.getBlockedChannels()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(channels -> {
                    adapter.setItems(channels);
                    updateEmptyState(channels.isEmpty());
                }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    private void updateEmptyState(final boolean isEmpty) {
        if (binding == null) {
            return;
        }
        binding.emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.blockedChannelsList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void confirmUnblock(final BlockedChannelEntity channel) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.unblock_channel_question)
                .setMessage(channel.getName() != null ? channel.getName() : channel.getUrl())
                .setPositiveButton(R.string.unblock_channel, (dialog, which) -> {
                    disposables.add(blockedChannelManager.unblockChannel(channel.getUrl())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> Toast.makeText(requireContext(),
                                    R.string.channel_unblocked, Toast.LENGTH_SHORT).show()));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Adapter
    //////////////////////////////////////////////////////////////////////////*/

    private final class BlockedChannelsAdapter
            extends RecyclerView.Adapter<BlockedChannelsAdapter.ViewHolder> {

        private final List<BlockedChannelEntity> items = new ArrayList<>();

        void setItems(final List<BlockedChannelEntity> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final ItemBlockedChannelBinding itemBinding =
                    ItemBlockedChannelBinding.inflate(
                            LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
            final BlockedChannelEntity channel = items.get(position);
            holder.bind(channel);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemBlockedChannelBinding itemBinding;

            ViewHolder(final ItemBlockedChannelBinding binding) {
                super(binding.getRoot());
                this.itemBinding = binding;
            }

            void bind(final BlockedChannelEntity channel) {
                itemBinding.blockedChannelName.setText(
                        channel.getName() != null ? channel.getName() : "Unknown");
                itemBinding.blockedChannelUrl.setText(channel.getUrl());
                itemBinding.unblockButton.setOnClickListener(v -> confirmUnblock(channel));
            }
        }
    }
}
