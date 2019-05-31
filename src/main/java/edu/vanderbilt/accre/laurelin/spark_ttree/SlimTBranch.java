package edu.vanderbilt.accre.laurelin.spark_ttree;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import edu.vanderbilt.accre.laurelin.Cache;
import edu.vanderbilt.accre.laurelin.array.ArrayBuilder;
import edu.vanderbilt.accre.laurelin.array.RawArray;
import edu.vanderbilt.accre.laurelin.root_proxy.Cursor;
import edu.vanderbilt.accre.laurelin.root_proxy.ROOTFile;
import edu.vanderbilt.accre.laurelin.root_proxy.TBranch;
import edu.vanderbilt.accre.laurelin.root_proxy.TFile;

/**
 * Contains all the info needed to read a TBranch and its constituent TBaskets
 * without needing to deserialize the ROOT metadata -- i.e. this contains paths
 * and byte offsets to each basket
 */
public class SlimTBranch implements Serializable {
    private static final long serialVersionUID = 1L;
    private String path;
    private long []basketEntryOffsets;
    private List<SlimTBasket> baskets;
    private TBranch.ArrayDescriptor arrayDesc;

    public SlimTBranch(String path, long []basketEntryOffsets, TBranch.ArrayDescriptor desc) {
        this.path = path;
        this.basketEntryOffsets = basketEntryOffsets;
        this.baskets = new LinkedList<SlimTBasket>();
        this.arrayDesc = desc;
    }

    public long [] getBasketEntryOffsets() {
        return basketEntryOffsets;
    }

    public SlimTBasket getBasket(int basketid) {
        return baskets.get(basketid);
    }

    public void addBasket(SlimTBasket basket) {
        baskets.add(basket);
    }

    public String getPath() {
        return path;
    }

    public TBranch.ArrayDescriptor getArrayDesc() {
        return arrayDesc;
    }

    /**
     * Glue callback to integrate with edu.vanderbilt.accre.laurelin.array
     *
     * @return GetBasket object used by array
     */
    public ArrayBuilder.GetBasket getArrayBranchCallback(Cache basketCache) {
        return new BranchCallback(basketCache, this);
    }

    class BranchCallback implements ArrayBuilder.GetBasket {
        Cache basketCache;
        SlimTBranch branch;

        public BranchCallback(Cache basketCache, SlimTBranch branch) {
            this.basketCache = basketCache;
            this.branch = branch;
        }

        @Override
        public ArrayBuilder.BasketKey basketkey(int basketid) {
            SlimTBasket basket = branch.getBasket(basketid);
            return new ArrayBuilder.BasketKey(basket.getKeyLen(), basket.getLast(), basket.getObjLen());
        }

        @Override
        public RawArray dataWithoutKey(int basketid) {
            SlimTBasket basket = branch.getBasket(basketid);
            try {
                // the last event of each basket is guaranteed to be unique and
                // stable
                TFile tmpFile = TFile.getFromFile(path);
                RawArray data = basketCache.get(tmpFile, basket.getLast());
                if (data == null) {
                    data = new RawArray(basket.getPayload());
                    basketCache.put(tmpFile, basket.getLast(), data);
                }
                return data;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class SlimTBasket implements Serializable {
        private static final long serialVersionUID = 1L;
        private SlimTBranch branch;
        private long offset;
        private int compressedLen;
        private int uncompressedLen;
        private int keyLen;
        private int last;
        private Cursor payload;

        public SlimTBasket(SlimTBranch branch, long offset, int compressedLen, int uncompressedLen, int keyLen, int last) {
            this.branch = branch;
            this.offset = offset;
            this.compressedLen = compressedLen;
            this.uncompressedLen = uncompressedLen;
            this.keyLen = keyLen;
            this.last = last;
        }

        public int getKeyLen() {
            return keyLen;
        }

        public int getObjLen() {
            return uncompressedLen;
        }

        public int getLast() {
            return last;
        }

        private void initializePayload() throws IOException {
            ROOTFile tmpFile = ROOTFile.getInputFile(branch.getPath());
            Cursor fileCursor = tmpFile.getCursor(offset);
            this.payload = fileCursor.getPossiblyCompressedSubcursor(0,
                    compressedLen,
                    uncompressedLen,
                    keyLen);
        }

        public ByteBuffer getPayload(long offset, int len) throws IOException {
            if (this.payload == null) {
                initializePayload();
            }
            return this.payload.readBuffer(offset, len);
        }

        public ByteBuffer getPayload() throws IOException {
            if (this.payload == null) {
                initializePayload();
            }
            long len = payload.getLimit();
            return this.payload.readBuffer(0, len);
        }

    }

}
