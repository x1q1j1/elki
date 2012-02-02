package experimentalcode.erich;

import java.util.Arrays;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Utilities for bit operations.
 * 
 * Implementation note: words are stored in little-endian word order. This can
 * be a bit confusing, because a shift-right means "left" on the word level.
 * 
 * Naming: methods with a <code>C</code> return a copy, methods with
 * <code>I</code> modify in-place.
 * 
 * @author Erich Schubert
 */
public final class BitsUtil {
  /**
   * Shift factor for a long: 2^6 == 64 == Long.SIZE
   */
  private static final int LONG_LOG2_SIZE = 6;

  /**
   * Masking for long shifts.
   */
  private static final int LONG_LOG2_MASK = 0x3f; // 6 bits

  /**
   * Long with all bits set
   */
  private static final long LONG_ALL_BITS = -1L;

  /**
   * Allocate a new long[].
   * 
   * @param bits Number of bits in storage
   * @return New array
   */
  public static long[] zero(int bits) {
    return new long[((bits - 1) >>> LONG_LOG2_SIZE) + 1];
  }

  /**
   * Allocate a new long[].
   * 
   * @param bits Number of bits in storage
   * @param init Initial value (of at most the size of a long, remaining bits
   *        will be 0)
   * @return New array
   */
  public static long[] make(int bits, long init) {
    long[] v = new long[((bits - 1) >>> LONG_LOG2_SIZE) + 1];
    v[0] = init;
    return v;
  }

  /**
   * Create a vector initialized with "bits" ones.
   * 
   * @param bits Size
   * @return new vector
   */
  public static long[] ones(int bits) {
    long[] v = new long[((bits - 1) >>> LONG_LOG2_SIZE) + 1];
    final int fillWords = bits >>> LONG_LOG2_SIZE;
    final int fillBits = bits & LONG_LOG2_MASK;
    Arrays.fill(v, 0, fillWords, LONG_ALL_BITS);
    v[v.length - 1] = (1L << fillBits) - 1;
    return v;
  }

  /**
   * Copy a bitset
   * 
   * @param v Array to copy
   * @return Copy
   */
  public static long[] copy(long[] v) {
    return Arrays.copyOf(v, v.length);
  }

  /**
   * Copy a bitset.
   * 
   * Note: Bits beyond mincap <em>may</em> be retained!
   * 
   * @param v Array to copy
   * @param mincap Target <em>minimum</em> capacity
   * @return Copy with space for at least "capacity" bits
   */
  public static long[] copy(long[] v, int mincap) {
    int words = ((mincap - 1) >>> LONG_LOG2_SIZE) + 1;
    if(v.length != words) {
      return Arrays.copyOf(v, v.length);
    }
    else {
      long[] ret = new long[words];
      System.arraycopy(v, 0, ret, 0, Math.min(v.length, words));
      return ret;
    }
  }

  /**
   * Compute corresponding gray code as v XOR (v >>> 1)
   * 
   * @param v Value
   * @return Gray code
   */
  public static long grayC(long v) {
    return v ^ (v >>> 1);
  }

  /**
   * Compute corresponding gray code as v XOR (v >>> 1)
   * 
   * @param v Value
   * @return Gray code
   */
  public static long[] grayI(long[] v) {
    // TODO: copy less
    long[] t = copy(v);
    shiftRightI(t, 1);
    xorI(v, t);
    return v;
  }

  /**
   * Compute the inverted gray code, v XOR (v >>> 1) XOR (v >>> 2) ...
   * 
   * @param v Value
   * @return Inverted gray code
   */
  public static long invgrayC(long v) {
    v ^= (v >>> 1);
    v ^= (v >>> 2);
    v ^= (v >>> 4);
    v ^= (v >>> 8);
    v ^= (v >>> 16);
    v ^= (v >>> 32);
    return v;
  }

  /**
   * Compute the inverted gray code, v XOR (v >>> 1) XOR (v >>> 2) ...
   * 
   * @param v Value
   * @return Inverted gray code
   */
  public static long[] invgrayI(long[] v) {
    long[] t = copy(v);
    int i = 0;
    while(!isZero(t)) {
      shiftRightI(t, i);
      xorI(v, t);
      i <<= 1;
    }
    return v;
  }

  /**
   * Test for the bitstring to be all-zero.
   * 
   * @param v Bitstring
   * @return true when all zero
   */
  public static boolean isZero(long[] v) {
    for(int i = 0; i < v.length; i++) {
      if(v[i] != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Compute the cardinality (number of set bits)
   * 
   * @param v Value
   * @return Number of bits set in long
   */
  public static int cardinality(long v) {
    return Long.bitCount(v);
  }

  /**
   * Compute the cardinality (number of set bits)
   * 
   * Low-endian layout for the array.
   * 
   * @param v Value
   * @return Number of bits set in long[]
   */
  public static long cardinality(long[] v) {
    int sum = 0;
    for(int i = 0; i < v.length; i++) {
      sum += Long.bitCount(v[i]);
    }
    return sum;
  }

  /**
   * Invert bit number "off" in v.
   * 
   * @param v Buffer
   * @param off Offset to flip
   */
  public static long flipC(long v, int off) {
    v ^= (1L << off);
    return v;
  }

  /**
   * Invert bit number "off" in v.
   * 
   * Low-endian layout for the array.
   * 
   * @param v Buffer
   * @param off Offset to flip
   */
  public static long[] flipI(long[] v, int off) {
    int wordindex = off >>> LONG_LOG2_SIZE;
    v[wordindex] ^= (1L << off);
    return v;
  }

  /**
   * Set bit number "off" in v.
   * 
   * @param v Buffer
   * @param off Offset to set
   */
  public static long setC(long v, int off) {
    v |= (1L << off);
    return v;
  }

  /**
   * Set bit number "off" in v.
   * 
   * Low-endian layout for the array.
   * 
   * @param v Buffer
   * @param off Offset to set
   */
  public static long[] setI(long[] v, int off) {
    int wordindex = off >>> LONG_LOG2_SIZE;
    v[wordindex] |= (1L << off);
    return v;
  }

  /**
   * Clear bit number "off" in v.
   * 
   * @param v Buffer
   * @param off Offset to clear
   */
  public static long clearC(long v, int off) {
    v &= ~(1L << off);
    return v;
  }

  /**
   * Clear bit number "off" in v.
   * 
   * Low-endian layout for the array.
   * 
   * @param v Buffer
   * @param off Offset to clear
   */
  public static long[] clearI(long[] v, int off) {
    int wordindex = off >>> LONG_LOG2_SIZE;
    v[wordindex] &= ~(1L << off);
    return v;
  }

  /**
   * Set bit number "off" in v.
   * 
   * @param v Buffer
   * @param off Offset to set
   */
  public static boolean get(long v, int off) {
    return (v & (1L << off)) != 0;
  }

  /**
   * Set bit number "off" in v.
   * 
   * Low-endian layout for the array.
   * 
   * @param v Buffer
   * @param off Offset to set
   */
  public static boolean get(long[] v, int off) {
    int wordindex = off >>> LONG_LOG2_SIZE;
    return (v[wordindex] & (1L << off)) != 0;
  }

  /**
   * Zero the given set
   * 
   * Low-endian layout for the array.
   * 
   * @param v existing set
   * @return array set to zero
   */
  public static long[] zeroI(long[] v) {
    Arrays.fill(v, 0);
    return v;
  }

  /**
   * XOR o onto v inplace, i.e. v ^= o
   * 
   * @param v Primary object
   * @param o data to xor
   * @return v
   */
  public static long[] xorI(long[] v, long[] o) {
    assert (o.length <= v.length) : "Bit set sizes do not agree.";
    for(int i = 0; i < o.length; i++) {
      v[i] ^= o[i];
    }
    return v;
  }

  /**
   * XOR o onto v inplace, i.e. v ^= (o << off)
   * 
   * @param v Primary object
   * @param o data to or
   * @param off Offset
   * @return v
   */
  public static long[] xorI(long[] v, long[] o, int off) {
    final int mag = magnitude(o);
    // TODO: optimize to not copy!
    long[] tmp = copy(o, mag + off);
    shiftLeftI(tmp, off);
    xorI(v, tmp);
    return v;
  }

  /**
   * OR o onto v inplace, i.e. v |= o
   * 
   * @param v Primary object
   * @param o data to or
   * @return v
   */
  public static long[] orI(long[] v, long[] o) {
    assert (o.length <= v.length) : "Bit set sizes do not agree.";
    for(int i = 0; i < o.length; i++) {
      v[i] |= o[i];
    }
    return v;
  }

  /**
   * OR o onto v inplace, i.e. v |= (o << off)
   * 
   * @param v Primary object
   * @param o data to or
   * @param off Offset
   * @return v
   */
  public static long[] orI(long[] v, long[] o, int off) {
    final int mag = magnitude(o);
    // TODO: optimize to not copy!
    long[] tmp = copy(o, mag + off);
    shiftLeftI(tmp, off);
    orI(v, tmp);
    return v;
  }

  /**
   * AND o onto v inplace, i.e. v &= o
   * 
   * @param v Primary object
   * @param o data to and
   * @return v
   */
  public static long[] andI(long[] v, long[] o) {
    int i = 0;
    for(; i < o.length; i++) {
      v[i] |= o[i];
    }
    // Zero higher words
    Arrays.fill(v, i, v.length, 0);
    return v;
  }

  /**
   * AND o onto v inplace, i.e. v &= (o << off)
   * 
   * @param v Primary object
   * @param o data to or
   * @param off Offset
   * @return v
   */
  public static long[] andI(long[] v, long[] o, int off) {
    final int mag = magnitude(o);
    // TODO: optimize to not copy!
    long[] tmp = copy(o, mag + off);
    shiftLeftI(tmp, off);
    andI(v, tmp);
    return v;
  }

  /**
   * Invert v inplace.
   * 
   * @param v Object to invert
   * @return v
   */
  public static long[] invertI(long[] v) {
    for(int i = 0; i < v.length; i++) {
      v[i] = ~v[i];
    }
    return v;
  }

  /**
   * Shift a long[] bitset inplace.
   * 
   * Low-endian layout for the array.
   * 
   * @param v existing bitset
   * @param off Offset to shift by
   * @return Bitset
   */
  public static long[] shiftRightI(long[] v, int off) {
    if(off == 0) {
      return v;
    }
    if(off < 0) {
      return shiftLeftI(v, -off);
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >>> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return zeroI(v);
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      // Move whole words down
      System.arraycopy(v, shiftWords, v, 0, v.length - shiftWords);
      // Fill top words with zeros
      Arrays.fill(v, v.length - shiftWords, v.length, 0);
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    // Bottom-up to not overlap the operations.
    for(int i = 0; i < v.length - shiftWords - 1; i++) {
      final int src = i + shiftWords;
      v[i] = (v[src + 1] << unshiftBits) | (v[src] >>> shiftBits);
    }
    // The last original word
    v[v.length - shiftWords - 1] = v[v.length - 1] >>> shiftBits;
    // Fill whole words "lost" by the shift
    Arrays.fill(v, v.length - shiftWords, v.length, 0);
    return v;
  }

  /**
   * Shift a long[] bitset inplace.
   * 
   * Low-endian layout for the array.
   * 
   * @param v existing bitset
   * @param off Offset to shift by
   * @return Bitset
   */
  public static long[] shiftLeftI(long[] v, int off) {
    if(off == 0) {
      return v;
    }
    if(off < 0) {
      return shiftRightI(v, -off);
    }
    // Break shift into integers to shift and bits to shift
    final int shiftWords = off >>> LONG_LOG2_SIZE;
    final int shiftBits = off & LONG_LOG2_MASK;

    if(shiftWords >= v.length) {
      return zeroI(v);
    }
    // Simple case - multiple of word size
    if(shiftBits == 0) {
      // Move whole words up
      System.arraycopy(v, 0, v, shiftWords, v.length - shiftWords);
      // Fill the initial words with zeros
      Arrays.fill(v, 0, shiftWords, 0);
      return v;
    }
    // Overlapping case
    final int unshiftBits = Long.SIZE - shiftBits;
    // Top-Down to not overlap the operations.
    for(int i = v.length - 1; i > shiftWords; i--) {
      final int src = i - shiftWords;
      v[i] = (v[src] << shiftBits) | (v[src - 1] >>> unshiftBits);
    }
    v[shiftWords] = v[0] << shiftBits;
    // Fill the initial words with zeros
    Arrays.fill(v, 0, shiftWords, 0);
    return v;
  }

  /**
   * Rotate a long to the right, cyclic with length len
   * 
   * @param v Bits
   * @param shift Shift value
   * @param len Length
   * @return cycled bit set
   */
  public static long cycleRightC(long v, int shift, int len) {
    if(shift == 0) {
      return v;
    }
    if(shift < 0) {
      return cycleLeftC(v, -shift, len);
    }
    final long ones = (1 << len) - 1;
    return (((v) >>> (shift)) | ((v) << ((len) - (shift)))) & ones;
  }

  /**
   * Cycle a bitstring to the right.
   * 
   * @param v Bit string
   * @param shift Number of steps to cycle
   * @param len Length
   */
  public static long[] cycleRightI(long[] v, int shift, int len) {
    // TODO: optimize - copy less
    long[] t = copy(v);
    shiftRightI(v, shift);
    shiftLeftI(t, len - shift);
    truncateI(t, len);
    orI(v, t);
    return v;
  }

  /**
   * Truncate a bit string to the given length (setting any higher bit to 0).
   * 
   * @param v String to process
   * @param len Length (in bits) to truncate to
   */
  public static long[] truncateI(long[] v, int len) {
    final int zap = (v.length * Long.SIZE) - len;
    final int zapWords = (zap >>> LONG_LOG2_SIZE);
    Arrays.fill(v, v.length - zapWords, v.length, 0);
    final int zapbits = zap & LONG_LOG2_MASK;
    if(zapbits > 0) {
      v[v.length - zapWords - 1] &= (LONG_ALL_BITS >>> zapbits);
    }
    return v;
  }

  /**
   * Rotate a long to the left, cyclic with length len
   * 
   * @param v Bits
   * @param shift Shift value
   * @param len Length
   * @return cycled bit set
   */
  public static long cycleLeftC(long v, int shift, int len) {
    if(shift == 0) {
      return v;
    }
    if(shift < 0) {
      return cycleRightC(v, -shift, len);
    }
    final long ones = (1 << len) - 1;
    return (((v) << (shift)) | ((v) >>> ((len) - (shift)))) & ones;
  }

  /**
   * Cycle a bitstring to the right.
   * 
   * @param v Bit string
   * @param shift Number of steps to cycle
   * @param len Length
   */
  public static long[] cycleLeftI(long[] v, int shift, int len) {
    // TODO: optimize - copy less
    long[] t = copy(v);
    shiftLeftI(v, shift);
    truncateI(v, len);
    shiftRightI(t, len - shift);
    orI(v, t);
    return v;
  }

  /**
   * Convert bitset to a string consisting of "0" and "1", in high-endian order.
   * 
   * @param v Value to process
   * @return String representation
   */
  public static String toString(long[] v) {
    final int mag = magnitude(v);
    final int words = ((mag - 1) >>> LONG_LOG2_SIZE) + 1;
    char[] digits = new char[mag];

    int pos = mag - 1;
    for(int w = 0; w < words; w++) {
      long f = 1l;
      for(int i = 0; i < Long.SIZE; i++) {
        digits[pos] = ((v[w] & f) == 0) ? '0' : '1';
        pos--;
        f <<= 1;
        if(pos < 0) {
          break;
        }
      }
    }
    return new String(digits);
  }

  /**
   * Find the number of trailing zeros.
   * 
   * @param v Bitset
   * @return Position of first set bit, -1 if no one was found.
   */
  public static int numberOfTrailingZeros(long[] v) {
    for(int p = 0;; p++) {
      if(p == v.length) {
        return -1;
      }
      if(v[p] != 0) {
        return Long.numberOfTrailingZeros(v[p]) + p * Long.SIZE;
      }
    }
  }

  /**
   * Find the number of leading zeros.
   * 
   * @param v Bitset
   * @return Position of first set bit, -1 if no one was found.
   */
  public static int numberOfLeadingZeros(long[] v) {
    for(int p = 0;; p++) {
      if(p == v.length) {
        return -1;
      }
      final int ip = v.length - 1 - p;
      if(v[ip] != 0) {
        return Long.numberOfLeadingZeros(v[ip]) + p * Long.SIZE;
      }
    }
  }

  /**
   * The magnitude is the position of the highest bit set
   * 
   * @param v Vector v
   * @return position of highest bit set, or 0.
   */
  public static int magnitude(long[] v) {
    return capacity(v) - numberOfLeadingZeros(v);
  }

  /**
   * Capacity of the vector v.
   * 
   * @param v Vector v
   * @return Capacity
   */
  public static int capacity(long[] v) {
    return v.length * Long.SIZE;
  }

  /**
   * Compare two bitsets.
   * 
   * @param x First bitset
   * @param y Second bitset
   * @return Comparison result
   */
  public static int compare(long[] x, long[] y) {
    int p = Math.min(x.length, y.length) - 1;
    for(int i = x.length - 1; i > p; i--) {
      if(x[i] != 0) {
        return +1;
      }
    }
    for(int i = y.length - 1; i > p; i--) {
      if(y[i] != 0) {
        return -1;
      }
    }
    for(; p >= 0; p--) {
      final long xp = x[p];
      final long yp = y[p];
      if(xp != yp) {
        if(xp < 0) {
          if(yp < 0) {
            return -Long.compare(xp, yp);
          }
          else {
            return +1;
          }
        }
        else {
          if(yp < 0) {
            return -1;
          }
          else {
            return Long.compare(xp, yp);
          }
        }
      }
    }
    return 0;
  }
}