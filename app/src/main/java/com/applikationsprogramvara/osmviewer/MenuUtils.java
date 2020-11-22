package com.applikationsprogramvara.osmviewer;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class MenuUtils {

    public static class MenuBuilder {

        private final List<MenuItem> items;
        private final WeakReference<Context> context;
        private String title;

        public MenuBuilder(Context context) {
            this.context = new WeakReference<>(context);
            items = new ArrayList<>();
        }

        public MenuBuilder add(int titleID, Action action) {
            return add(context.get().getString(titleID), action);
        }

        public MenuBuilder add(String title, Action action) {
            items.add(new MenuItem(title, action));
            return this;
        }

        public MenuBuilder add(boolean show, int titleID, Action action) {
            return add(show, context.get().getString(titleID), action);
        }

        public MenuBuilder add(boolean show, String title, Action action) {
            if (show)
                return add(title, action);
            return this;
        }

        public MenuBuilder setTitle(int titleID) {
            return setTitle(context.get().getString(titleID));
        }

        public MenuBuilder setTitle(String title) {
            this.title = title;
            return this;
        }

        public void show() {
            final LinkedHashMap<Integer, String> items = new LinkedHashMap<>();
            for (int i = 0; i < this.items.size(); i++)
                items.put(i, this.items.get(i).title);

            AlertDialog.Builder builder = new AlertDialog.Builder(context.get());
            builder.setTitle(title);
            builder.setItems(items.values().toArray(new String[0]), (dialog, item) -> {
                int item_int = (Integer) items.keySet().toArray()[item];
                this.items.get(item_int).action.action();
            });
            builder.create().show();
        }

        public void show2(int checkedItem) {
            final LinkedHashMap<Integer, String> items = new LinkedHashMap<>();
            for (int i = 0; i < this.items.size(); i++)
                items.put(i, this.items.get(i).title);

            AlertDialog.Builder builder = new AlertDialog.Builder(context.get());
            builder.setTitle(title);
            builder.setSingleChoiceItems(items.values().toArray(new String[0]), checkedItem, (dialog, item) -> {
                int item_int = (Integer) items.keySet().toArray()[item];
                this.items.get(item_int).action.action();
                dialog.dismiss();
            });
            builder.create().show();
        }

        private class MenuItem {
            private final String title;
            private final Action action;

            public MenuItem(String title, Action action) {
                this.title = title;
                this.action = action;
            }
        }

        public interface Action {
            void action();
        }

    }


}
