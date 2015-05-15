{ prism => '../bin/prism'
, models =>
  [ { path => 'models/cluster.sm'
    , opts =>
      [ { parameters =>
          [ { ws_fail => [0.0025, 0.05], line_fail => [0.00025, 0.05] }
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
          [ { synth => 1, type => 'thr', csl => 'P<=0.01 [ F<=1000 !"minimum" ]', acc => 0.05 }
          ]
        }
      ]
    }
  , { path => 'models/signalling_2_v6_noreg.sm'
    , opts =>
      [ { parameters =>
          [ { k1 => [0.1, 0.5], k2 => [0.01, 0.015] }
          ]
        , environment =>
          [ {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1}
          , {PSE_OCL=>0,PSE_ADAPTIVE_FOX_GLYNN=>1,PSE_PARA=>4}
          ]
        , properties =>
          [ { synth => 1, type => 'thr', csl => 'P<0.4 [ F[100,100] (popRp<=20 & popHp>=30) ]', acc => 0.3 }
          ]
        }
      ]
    }
  ]
}