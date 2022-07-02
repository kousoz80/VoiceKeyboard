



# VoiceKeyboard
  
  本ソフトウェアはandroid端末用の音声キーボードソフトです。 
  
  端末に向かって話すとキーボードがタイプされます。
  
  
 
  
## ・操作方法
  
  
  本ソフトウェアは２つのプログラムからなっています
  
## VKeyboardControl
  このプログラムは使用者の音声データを登録するためのものです。
    
  プログラムを起動して、しばらくしてから何か話すと右側の窓に声紋が表示されます。
  
  
![enter image description here](image/config1.png?raw=true)  
  
![enter image description here](image/config2.png?raw=true)  
  
![enter image description here](image/config3.png?raw=true)  
  

## パラメータ設定画面
![enter image description here](image/properties.png?raw=true)  
  

## イコライザ画面
![enter image description here](image/equalizer.png?raw=true)  
  
## VkeyboardService  
  
  本ソフトウェアの本体です。
  "VKeyboardControl"で作成した音声データを使って音声を認識してキーボードを打ちます。
  
  プログラムを起動する前に端末をパソコンにUSBケーブルで接続してターミナルを開いて以下のコマンドをタイプします。
  
  adb tcpip 5555
  
  
  コマンドを打ち終わったらケーブルを抜いてからプログラムを起動します。  
  
![enter image description here](image/mukou1.png?raw=true)  
  
![enter image description here](image/mukou2.png?raw=true)  
  
サービス起動直後は音声キーボードは無効になっているので  
「ボイス」と発音すると通知アイコンが変化して音声キーボードが有効となります。  
  
![enter image description here](image/yuukou1.png?raw=true)  
  
![enter image description here](image/yuukou2.png?raw=true)  
  

音声学習について  
  
何か発音して結果が表示されたあと正解だったら「よし」と発音すると報酬として学習します。  
  
![enter image description here](image/yoshi.png?raw=true)  
  
不正解だったら「ちがう」と発音するとペナルティとして学習します。  
  
![enter image description here](image/chigau.png?raw=true)  
  
認識が失敗した場合にはそのように表示されます。  
  
![enter image description here](image/wakaran.png?raw=true)  
  

#### ・インストール方法
  
"VkeyboardControl.apk"と"VkeyboardService.apk"をファイルマネージャ等で開くとインストールが始まります。
  
    
#### ・コンパイル方法
  
#### 方法1
ObjectEditor](https://github.com/kousoz80/ObjectEditor)で"VkeyboadControl.prj"や"VkeyboadService.prj"  を開いてコンパイルボタンを押すとコンパイルが始まります
  
![enter image description here](image/compile1.png?raw=true)  

  
  
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
  
  

