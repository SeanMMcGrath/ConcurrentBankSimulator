package my_version;


import java.io.Serializable;

/**
 * @author SeanMcGrath
 */
public class HashMap implements Serializable {

     class Entry implements Serializable {

        final String key;
        int value;
        Entry next;
        int hash;

        Entry(String k, int v, Entry p, int h) {
            key = k;
            value = v;
            next = p;
            hash = h;
        }
    }

    Entry[] tab;
    int count = 0;

    public HashMap() {
        tab = new Entry[16];
    }

    public int get(String key) {
        int h = key.hashCode();
        int i = h & (tab.length - 1);

        for (Entry e = tab[i]; e != null; e = e.next) {
            if (e.hash == h && key.equals(e.key)) {
                return e.value;
            }
        }
        return -1;
    }

    public void put(String key, int value) {
        int h = key.hashCode();
        Entry[] t = tab;
        int i = h & (t.length - 1);
        for (Entry e = t[i]; e != null; e = e.next) {
            if (e.hash == h && key.equals(e.key)) {
                e.value = value;
                return;
            }
        }
        Entry p = new Entry(key, value, t[i], h);
        t[i] = p;
        int c = ++count;
        double f = t.length;
        if ((c / f) < 0.75) {
            return;
        }

        int n = t.length;
        int newN = n << 1;
        Entry[] newTab = new Entry[newN];
        for (int j = 0; j < n; ++j) {
            Entry e;
            while ((e = t[j]) != null) {
                t[j] = e.next;
                int k = e.hash & (newN - 1);
                e.next = newTab[k];
                newTab[k] = e;
            }
        }
        tab = newTab;
    }
}

