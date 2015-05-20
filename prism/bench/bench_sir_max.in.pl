{ prism => '../bin/prism'
, models =>
  [ { path => 'models/sir.sm'
    , opts =>
      [ { parameters =>
          [ { ki => [0.00005, 0.003], kr => [0.005, 0.2] }
          ]
        , environment =>
          [ {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>0}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>2}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>8}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>16}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>8}
          , {PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>16}
          ]
        , properties =>
          [ { synth => 1, type => 'max-sample', csl => 'P=? [ (popI>0) U[165,215] (popI=0) ]', acc => 0.2 }
          ]
        }
      ]
    }
  ]
}
