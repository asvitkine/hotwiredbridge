package org.freeshell.gbsmith.rescafe;

import java.awt.image.IndexColorModel;

/*=======================================================================*/
/* The MIT License

Copyright (c) 1999-2009 by G. Brannon Smith

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
/*=======================================================================*/

/*=======================================================================*/
public class MacStandard16Palette
{
   /*--- RCS ------------------------------------------------------------*/
   static final String rcsid = "$Id: MacStandard16Palette.java,v 1.6 1999/12/19 01:28:57 gbsmith Exp $";

   /*--- Data -----------------------------------------------------------*/
   static private final byte reds[] =
   {  -1,  -4,  -1,  -35,  -14,   70,   0,   2,
      31,   0,  86, -112,  -64, -128,  64,   0,
      0 };

   static private final byte greens[] =
   {  -1, -13, 100,    8,    8,    0,   0, -85,
     -73, 100,  44,  113,  -64, -128,  64,   0,
       0 };

   static private final byte blues[] =
   {  -1,   5,   2,    6, -124,  -91, -44, -22,
      20,  17,   5,   58,  -64, -128,  64,   0,
       0 };

   static private final int alpha = 16;

   /*--- Methods --------------------------------------------------------*/
   public static byte[] getReds()   { return reds; }
   public static byte[] getGreens() { return greens; }
   public static byte[] getBlues()  { return blues; }

   public static IndexColorModel getColorModel()
   {
      return new IndexColorModel(8, 17, reds, greens, blues, alpha );
   }
}
