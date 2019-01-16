module TEST
  imports FLOAT
  imports STRING

  rule 0 => 1.0f
  rule 1 => 2.0
  rule 2 => 3.0p100x10
  rule 3 => 4.0d

  rule 4.0f => -1
  rule 4.0p100x10 => -1

  rule 4 => 1.0
  rule 1.0p53x11 => 2.0p24x8
  rule 2.0f => 3.0

  rule 5 => 1.0 /Float 0.0
  rule -Infinity => -1
  rule 6 => 0.0
  rule -0.0 => -1
  rule 7 => 0.0f /Float 0.0f

  rule 8 => Float2String(String2Float("8.0f"))
endmodule
