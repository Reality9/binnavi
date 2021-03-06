/*
Copyright 2015 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.google.security.zynamics.reil.translators.x86;

import com.google.security.zynamics.reil.OperandSize;
import com.google.security.zynamics.reil.ReilHelpers;
import com.google.security.zynamics.reil.ReilInstruction;
import com.google.security.zynamics.reil.translators.IInstructionTranslator;
import com.google.security.zynamics.reil.translators.ITranslationEnvironment;
import com.google.security.zynamics.reil.translators.InternalTranslationException;
import com.google.security.zynamics.reil.translators.TranslationHelpers;
import com.google.security.zynamics.zylib.disassembly.IInstruction;
import com.google.security.zynamics.zylib.disassembly.IOperandTree;

import java.util.List;


/**
 * Translates LOOP instructions to REIL code.
 */
public class LoopTranslator implements IInstructionTranslator {

  /**
   * Translates a LOOP instruction to REIL code.
   * 
   * @param environment A valid translation environment.
   * @param instruction The LOOP instruction to translate.
   * @param instructions The generated REIL code will be added to this list
   * 
   * @throws InternalTranslationException if any of the arguments are null the passed instruction is
   *         not a LOOP instruction
   */
  @Override
  public void translate(final ITranslationEnvironment environment, final IInstruction instruction,
      final List<ReilInstruction> instructions) throws InternalTranslationException {
    TranslationHelpers.checkTranslationArguments(environment, instruction, instructions, "loop");

    if (instruction.getOperands().size() != 1) {
      throw new InternalTranslationException(
          "Error: Argument instruction is not a loop instruction (invalid number of operand)");
    }

    final long baseOffset = instruction.getAddress().toLong() * 0x100;

    final List<? extends IOperandTree> operands = instruction.getOperands();

    final OperandSize archSize = environment.getArchitectureSize();
    final OperandSize resultSize = TranslationHelpers.getNextSize(archSize);

    final String truncateMask = String.valueOf(TranslationHelpers.getAllBitsMask(archSize));

    final String loopTarget = Helpers.getLeafValue(operands.get(0).getRootNode());

    final String tempEcx = environment.getNextVariableString();

    // Decrement ECX and truncate overflows
    instructions.add(ReilHelpers.createSub(baseOffset, archSize, "ecx", archSize, "1", archSize,
        tempEcx));
    instructions.add(ReilHelpers.createAnd(baseOffset + 1, resultSize, tempEcx, archSize,
        truncateMask, archSize, "ecx"));

    // Jump if ECX != 0
    instructions.add(ReilHelpers.createJcc(baseOffset + 2, archSize, "ecx", archSize, loopTarget));
  }

}
