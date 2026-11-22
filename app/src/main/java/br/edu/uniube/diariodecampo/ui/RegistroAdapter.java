package br.edu.uniube.diariodecampo.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.edu.uniube.diariodecampo.databinding.ItemRegistroBinding;
import br.edu.uniube.diariodecampo.model.RegistroCampo;

public class RegistroAdapter extends RecyclerView.Adapter<RegistroAdapter.ViewHolder> {

    public interface OnRegistroClickListener {
        void onRegistroClick(RegistroCampo registro);
    }

    private final List<RegistroCampo> itens = new ArrayList<>();
    private final OnRegistroClickListener listener;

    public RegistroAdapter(OnRegistroClickListener listener) {
        this.listener = listener;
    }

    public void atualizarLista(List<RegistroCampo> novosItens) {
        itens.clear();
        if (novosItens != null) {
            itens.addAll(novosItens);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRegistroBinding binding = ItemRegistroBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegistroCampo item = itens.get(position);
        holder.binding.tvItemTitulo.setText(item.getTitulo());
        holder.binding.tvItemData.setText("Data: " + item.getDataHora());
        holder.binding.tvItemCoordenadas.setText(String.format(Locale.US,
                "GPS: Lat %.5f | Lon %.5f (Luz: %.1f lx)",
                item.getLatitude(), item.getLongitude(), item.getLuminosidadeLux()));

        if (item.getFotoMiniatura() != null) {
            holder.binding.ivItemFoto.setImageBitmap(item.getFotoMiniatura());
        } else {
            holder.binding.ivItemFoto.setImageResource(android.R.drawable.ic_menu_camera);
        }

        if (item.getDadosEnriquecidosApi() != null && !item.getDadosEnriquecidosApi().isEmpty()) {
            holder.binding.tvItemApiInfo.setVisibility(View.VISIBLE);
            holder.binding.tvItemApiInfo.setText(item.getDadosEnriquecidosApi());
        } else {
            holder.binding.tvItemApiInfo.setVisibility(View.VISIBLE);
            holder.binding.tvItemApiInfo.setText("Toque para enriquecer dados via API REST");
        }

        holder.itemView.setOnClickListener(v -> listener.onRegistroClick(item));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRegistroBinding binding;
        ViewHolder(ItemRegistroBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
