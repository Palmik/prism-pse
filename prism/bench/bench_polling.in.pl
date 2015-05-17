{ prism => '../bin/prism'
, models =>
  [ { path => 'models/polling.sm'
    , opts =>
      [ { parameters =>
          [ { gamma => [50, 400], mu => [0.5,4] }
          ]
        , environment =>
          [ {PSE_OCL=>1,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>1}
          , {PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>2}
          , {PSE_OCL=>1,PSE_FMT=>'CSR',PSE_MANY=>4}
          , {PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>1}
          , {PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>2}
          , {PSE_OCL=>1,PSE_FMT=>'ELL',PSE_MANY=>4}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>0}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>4}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'R{"waiting"}<=0.75[C<=10]', acc => 0.05 }
          ]
        }
      ]
    }
  ]
}
