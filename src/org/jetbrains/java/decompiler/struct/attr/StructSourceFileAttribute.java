// Copyright 2021 https://github.com/bianchui/. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.attr;

import org.jetbrains.java.decompiler.struct.consts.ConstantPool;
import org.jetbrains.java.decompiler.util.DataInputFullStream;

import java.io.IOException;

public class StructSourceFileAttribute extends StructGeneralAttribute {

  private String sourceFile;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool) throws IOException {
    int index = data.readUnsignedShort();
    sourceFile = pool.getPrimitiveConstant(index).getString();
  }

  public String getSourceFile() {
    return sourceFile;
  }
}
