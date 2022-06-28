



# VoiceKeyboard
  
  本ソフトウェアはandroid端末用の音声キーボードソフトです。 
  
  端末に向かって話すとキーボードがタイプされます。
  
  
 
  
## ・操作方法
  
  
  本ソフトウェアは２つのプログラムからなっています
  
### １． VKeyboardControl
  このプログラムは使用者の音声データを登録するためのものです。
    
  プログラムを起動して、しばらくしてから何か話すと右側の窓に声紋が表示されます。
  
  そのとき"STORE"ボタンを押すと話した音声が記録されます。
  
  そうすると登録した音声の声紋が左側の窓に表示されます。
  
  "CODE"と書いてある所にキーボードのコードが表示されています。
  
  全てのコードの登録が終わったら"SAVE"ボタンを押して音声データを保存して"戻る"ボタンで終了して下さい。
  
  ※注意：使用者や端末を変更すると以前のデータは使えなくなります。
  
    
![enter image description here](https://imgur.com/o8jn2To.jpg)


### ２．VkeyboardService  
  
  本ソフトウェアの本体です。
  "VKeyboardControl"で作成した音声データを使って音声を認識してキーボードを打ちます。
  
  プログラムを起動する前に端末をパソコンにUSBケーブルで接続してターミナルを開いて以下のコマンドをタイプします。
  
  adb tcpip 5555
  
  
  コマンドを打ち終わったらケーブルを抜いてからプログラムを起動します。
    
  
  
#### ・インストール方法
  
"VkeyboardControl.apk"と"VkeyboardService.apk"をファイルマネージャ等で開くとインストールが始まります。
  
    
#### ・コンパイル方法
  
#### 方法1
ObjectEditor](https://github.com/kousoz80/ObjectEditor)で"VkeyboadControl.prj"や"VkeyboadService.prj"  を開いてコンパイルボタンを押すとコンパイルが始まります
  
![enter image description here](https://imgur.com/J2oDqz5.jpg)

  
  
#### 方法2
ディレクトリ"VoiceKeyboardControl"や"VoiceKeyboardService"に入って"./compile"コマンドでコンパイルすることができます。

コンパイルする前にandroidプラットフォームのディレクトリからファイル"android.jar"をこのディレクトリにコピーして下さい。


  
## ・動作要件
  自分自身にADB接続できるandroid端末が必要です。
  
  それとプログラムの起動のためのパソコンが必要となります。
  
## ・動画  
Youtubeで動画が公開されています  

https://youtu.be/GDhVQUwxKw4
  
  
## ・謝辞
   
   このソフトウェアを世界中の身体が不自由な方々に捧げます。
   
   営利・非営利問わずどのように利用されてもかまいません。
   
   このソフトウェアが大勢の方々のお役に立つことを願っています。
  
  

