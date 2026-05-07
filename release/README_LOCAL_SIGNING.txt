【このフォルダについて】

■ ここに置くもの（手元の PC だけ・Git には上がりません）
  - upload.jks … リリース署名用キーストア
  - それ以外、署名関連でローカルに置きたいファイル

■ パスワード等の設定
  プロジェクトの「一番上」のフォルダにある
    keystore.properties
  に storeFile / storePassword / keyAlias / keyPassword を書きます（Git に含めない）。
  雛形はリポジトリ直下の keystore.properties.example をコピーして編集。

■ バックアップ
  upload.jks と keystore.properties は USB などに必ずコピーしてください。
  無くすと同じ鍵でアプリを更新できなくなることがあります。

■ push 前の確認（推奨）
  PowerShell:  git status
  で keystore.properties や *.jks が「変更として出ていない」ことを確認してください。
  万一出ていたら add しないでください。
