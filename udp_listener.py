import socket

def main():
    # Создаем UDP сокет
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    
    # Привязываем к порту 3004 на всех интерфейсах
    sock.bind(('0.0.0.0', 3004))
    
    print("UDP listener started on port 3004. Waiting for messages...")
    print("Press Ctrl+C to stop the program.")
    
    try:
        while True:
            # Получаем данные (максимум 1024 байта)
            data, addr = sock.recvfrom(1024)
            print(data.hex())
            # Если данные в hex, можно распечатать
           
            # Или как строка, если текст
  
    except KeyboardInterrupt:
        print("Stopping listener...")
    finally:
        sock.close()

if __name__ == "__main__":
    main()