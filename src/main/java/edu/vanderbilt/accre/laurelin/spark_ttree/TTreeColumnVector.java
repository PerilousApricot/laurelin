package edu.vanderbilt.accre.laurelin.spark_ttree;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.Decimal;
import org.apache.spark.sql.vectorized.ColumnVector;
import org.apache.spark.sql.vectorized.ColumnarArray;
import org.apache.spark.sql.vectorized.ColumnarMap;
import org.apache.spark.unsafe.types.UTF8String;

import edu.vanderbilt.accre.laurelin.Cache;
import edu.vanderbilt.accre.laurelin.array.Array;
import edu.vanderbilt.accre.laurelin.array.ArrayBuilder;
import edu.vanderbilt.accre.laurelin.interpretation.AsDtype;
import edu.vanderbilt.accre.laurelin.interpretation.AsDtype.Dtype;
import edu.vanderbilt.accre.laurelin.interpretation.AsJagged;
import edu.vanderbilt.accre.laurelin.interpretation.Interpretation;
import edu.vanderbilt.accre.laurelin.root_proxy.SimpleType;
import edu.vanderbilt.accre.laurelin.root_proxy.TBranch;

public class TTreeColumnVector extends ColumnVector {
    private long [] basketEntryOffsets;
    private ArrayBuilder.GetBasket getbasket;
    private ArrayBuilder builder;

    public static Dtype sparkTypeToDType(DataType sparkType) {

        throw new RuntimeException("boo");
        //return AsDtype.Dtype.FLOAT4;
    }

    public TTreeColumnVector(DataType type, SimpleType rootType, Dtype dtype, Cache basketCache, long entrystart, long entrystop, SlimTBranch slimBranch) {
        super(type);

        this.basketEntryOffsets = slimBranch.getBasketEntryOffsets();
        this.getbasket = slimBranch.getArrayBranchCallback(basketCache);

        ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);

        TBranch.ArrayDescriptor desc = slimBranch.getArrayDesc();
        if (desc == null) {
            Interpretation interpretation = new AsDtype(dtype);   // FIXME
            this.builder = new ArrayBuilder(getbasket, interpretation, basketEntryOffsets, executor, entrystart, entrystop);
        }
        else if (desc.isFixed()) {
            Interpretation interpretation = new AsDtype(dtype, Arrays.asList(desc.getFixedLength()));   // FIXME
            this.builder = new ArrayBuilder(getbasket, interpretation, basketEntryOffsets, executor, entrystart, entrystop);
        } else {
            Interpretation interpretation = new AsJagged(new AsDtype(dtype));   // FIXME
            this.builder = new ArrayBuilder(getbasket, interpretation, basketEntryOffsets, executor, entrystart, entrystop);
        }
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean hasNull() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int numNulls() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isNullAt(int rowId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean getBoolean(int rowId) {
        return getBooleans(rowId, 1)[0];
    }

    @Override
    public byte getByte(int rowId) {
        return getBytes(rowId, 1)[0];
    }

    @Override
    public short getShort(int rowId) {
        return getShorts(rowId, 1)[0];
    }

    @Override
    public int getInt(int rowId) {
        return getInts(rowId, 1)[0];
    }

    @Override
    public long getLong(int rowId) {
        return getLongs(rowId, 1)[0];
    }

    @Override
    public float getFloat(int rowId) {
        return getFloats(rowId, 1)[0];
    }

    @Override
    public double getDouble(int rowId) {
        // TODO Auto-generated method stub
        return getDoubles(rowId, 1)[0];
    }

    @Override
    public ColumnarArray getArray(int rowId) {
        Array array = builder.getArray(rowId, 1).subarray();
        return new ColumnarArray(new ArrayColumnVector(((ArrayType)dataType()).elementType(), array), 0, array.length());
    }

    @Override
    public ColumnarMap getMap(int ordinal) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Decimal getDecimal(int rowId, int precision, int scale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UTF8String getUTF8String(int rowId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] getBinary(int rowId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected ColumnVector getChild(int ordinal) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * Return
     */

    @Override
    public boolean[] getBooleans(int rowId, int count) {
        return (boolean[])(builder.getArray(rowId, count).toArray());
    }

    @Override
    public byte[] getBytes(int rowId, int count) {
        return (byte[])(builder.getArray(rowId, count).toArray());
    }

    @Override
    public short[] getShorts(int rowId, int count) {
        return (short[])(builder.getArray(rowId, count).toArray());
    }

    @Override
    public int[] getInts(int rowId, int count) {
        return (int[])(builder.getArray(rowId, count).toArray());
    }

    @Override
    public long[] getLongs(int rowId, int count) {
        return (long[])(builder.getArray(rowId, count).toArray());
    }

    @Override
    public float[] getFloats(int rowId, int count) {
        return (float[])(builder.getArray(rowId, count).toArray());
    }

    @Override
    public double[] getDoubles(int rowId, int count) {
        return (double[])(builder.getArray(rowId, count).toArray());
    }
}
