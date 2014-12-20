{ prism => '../bin/prism'
, models =>
  [ { path => 'models/signalling_2_v6_noreg.sm'
    , parameters =>
      [ { value => 'k1=1:1.005,k2=1.995:2 5'
        , options =>
          [ { pse => { time => 20 } }
          , { pse => { time => 40 } }
          , { pse => { time => 80 } }
          , { psecheck => { csl => 'P=? [ F=40 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=80 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=160 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=320 popR = 2 ]' } }
          ]
        } 
      , { value => 'k1=1:1,k2=2:2 5'
        , options =>
          [ { pse => { time => 40 } }
          , { pse => { time => 80 } }
          , { psecheck => { csl => 'P=? [ F=160 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=320 popR = 2 ]' } }
          ]
        } 
      , { value => 'k1=2:2.005,k2=3.995:4 5'
        , options =>
          [ { pse => { time => 20 } }
          , { pse => { time => 40 } }
          , { pse => { time => 80 } }
          , { psecheck => { csl => 'P=? [ F=40 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=80 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=160 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=320 popR = 2 ]' } }
          ]
        }
      , { value => 'k1=2:2,k2=4:4 5'
        , options =>
          [ { pse => { time => 40 } }
          , { pse => { time => 80 } }
          , { psecheck => { csl => 'P=? [ F=160 popR = 2 ]' } }
          , { psecheck => { csl => 'P=? [ F=320 popR = 2 ]' } }
          ]
        }
      ]
    }
  , { path => 'models/sir.sm'
    , parameters =>
      [ { value => 'ki=0.003:0.0035,kr=0.2:0.205 10'
        , options =>
          [ { pse => { time => 10 } }
          , { pse => { time => 20 } }
          , { pse => { time => 40 } }
          , { psecheck => { csl => 'P=? [popI>0 U[100,120] popI=0]' } }
          , { psecheck => { csl => 'P=? [popI>0 U[100,140] popI=0]' } }
          , { psecheck => { csl => 'P=? [popI>0 U[100,180] popI=0]' } }
          ]
        }
      , { value => 'ki=0.003:0.003,kr=0.2:0.2 10'
        , options =>
          [ { pse => { time => 10 } }
          , { pse => { time => 20 } }
          , { pse => { time => 40 } }
          , { psecheck => { csl => 'P=? [popI>0 U[100,120] popI=0]' } }
          , { psecheck => { csl => 'P=? [popI>0 U[100,140] popI=0]' } }
          , { psecheck => { csl => 'P=? [popI>0 U[100,180] popI=0]' } }
          ]
        } 
      ]
    }
  , { path => 'models/g1s.sm'
    , parameters =>
      [ { value => 'degr=0.1:0.105,prod=1:1.005 2'
        , options =>
          [ { pse => { time => 2 } }
          , { pse => { time => 4 } }
          , { pse => { time => 8 } }
          , { pse => { time => 16 } }
          , { psecheck => { csl => 'P=? [AA < B U[1,2] AA > B]' } }
          , { psecheck => { csl => 'P=? [AA < B U[1,4] AA > B]' } }
          , { psecheck => { csl => 'P=? [AA < B U[1,8] AA > B]' } }
          , { psecheck => { csl => 'P=? [AA < B U[1,16] AA > B]' } }
          ]
        }
      , { value => 'degr=0.1:0.1,prod=1:1 2'
        , options =>
          [ { pse => { time => 8 } }
          , { pse => { time => 16 } }
          , { psecheck => { csl => 'P=? [AA < B U[1,4] AA > B]' } }
          , { psecheck => { csl => 'P=? [AA < B U[1,8] AA > B]' } }
          ]
        }
      ]
    }
  , { path => 'models/gene-reg.sm'
    , parameters =>
      [ { value => 'k1=0.17:0.17001 20'
        , options =>
          [ { pse => { time => 0.5625 } }
          , { pse => { time => 1.125 } }
          , { pse => { time => 2.5 } }
          , { pse => { time => 5 } }
          , { psecheck => { csl => 'P=? [F=40 RbsRibosome = 1]' } }
          , { psecheck => { csl => 'P=? [F=80 RbsRibosome = 2]' } }
          , { psecheck => { csl => 'P=? [F=160 RbsRibosome = 2]' } }
          , { psecheck => { csl => 'P=? [F=320 RbsRibosome = 2]' } }
          ]
        }
      , { value => 'k1=0.17:0.17001 20'
        , options =>
          [ { pse => { time => 0.5625 } }
          , { pse => { time => 1.125 } }
          , { pse => { time => 2.5 } }
          , { pse => { time => 5 } }
          , { psecheck => { csl => 'P=? [F=40 RbsRibosome = 1]' } }
          , { psecheck => { csl => 'P=? [F=80 RbsRibosome = 2]' } }
          , { psecheck => { csl => 'P=? [F=160 RbsRibosome = 2]' } }
          , { psecheck => { csl => 'P=? [F=320 RbsRibosome = 2]' } }
          ]
        }
      ]
    }
  , { path => 'models/google.sm'
    , parameters =>
      [ { value => 'repair_rate=0.95:1 20'
        , options =>
          [ { pse => { time => 2.5 } }
          , { pse => { time => 5 } }
          , { pse => { time => 10 } }
          , { pse => { time => 20 } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,10] service_level_3], disaster)' } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,20] service_level_3], disaster)' } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,40] service_level_3], disaster)' } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,80] service_level_3], disaster)' } }
          ]
        }
      , { value => 'repair_rate=1:1 20'
        , options =>
          [ { pse => { time => 2.5 } }
          , { pse => { time => 5 } }
          , { pse => { time => 10 } }
          , { pse => { time => 20 } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,10] service_level_3], disaster)' } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,20] service_level_3], disaster)' } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,40] service_level_3], disaster)' } }
          , { psecheck => { csl => 'filter(print, P=? [ F[0,80] service_level_3], disaster)' } }
          ]
        }
      ]
    }
  ]
}
