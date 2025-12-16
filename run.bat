llvm-link llvm_ir.txt libsysy/lib.ll -S -o llvm_out/out.ll
lli .\llvm_out\out.ll